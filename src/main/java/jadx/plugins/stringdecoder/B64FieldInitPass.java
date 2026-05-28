package jadx.plugins.stringdecoder;

import java.util.TreeMap;

import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.FieldInitInsnAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64FieldInitPass implements JadxDecompilePass {

	private static final int MAX_ARG_TREE_DEPTH = 8;

	private final B64DeobfuscateOptions options;

	public B64FieldInitPass(B64DeobfuscateOptions options) {
		this.options = options;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"B64FieldInitDeobfuscate",
				"Detect and decode likely Base64-encoded static field initializers")
				.after("ExtractFieldInit");
	}

	@Override
	public void init(RootNode root) {
	}

	@Override
	public boolean visit(ClassNode cls) {
		for (FieldNode field : cls.getFields()) {
			processField(field);
		}
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
	}

	private void processField(FieldNode field) {
		FieldInitInsnAttr initAttr = field.get(AType.FIELD_INIT_INSN);
		if (initAttr != null) {
			processExtractedInit(field, initAttr.getInsn());
			return;
		}
		// static final field with literal value encoded in the class file (no <clinit>).
		String constStr = FieldConstants.readStringValue(field);
		if (constStr != null) {
			annotateField(field, constStr, false);
		}
	}

	/** Dispatch on the shape of the extracted init instruction. */
	private void processExtractedInit(FieldNode field, InsnNode initInsn) {
		if (initInsn instanceof ConstStringNode) {
			annotateField(field, ((ConstStringNode) initInsn).getString(), false);
			return;
		}
		if (initInsn instanceof FilledNewArrayNode) {
			FilledNewArrayNode arr = (FilledNewArrayNode) initInsn;
			// B64DeobfuscatePass already handled the original filled-new-array — skip if commented.
			if (!arr.contains(AType.CODE_COMMENTS)) {
				annotateFilledArray(field, arr);
			}
			return;
		}
		// Complex init expression (e.g. `new String(Base64.decode("...", 0))`) — walk arg tree.
		findAndAnnotateInArgTree(field, initInsn, 0);
	}

	/**
	 * Indexed, anchor-based detection for filled-new-array field inits. Emits a comment only when
	 * at least one element passes full detection (the anchor); every valid Base64+UTF-8 element is
	 * then included with its array index.
	 */
	private void annotateFilledArray(FieldNode field, FilledNewArrayNode filledArray) {
		TreeMap<Integer, B64Result> candidates = new TreeMap<>();
		boolean hasAnchor = false;
		for (int idx = 0; idx < filledArray.getArgsCount(); idx++) {
			String str = extractStringFromArg(field, filledArray.getArg(idx));
			if (str == null || B64FalsePositives.contains(str)) {
				continue;
			}
			B64Result full = B64Detector.detect(str, options);
			B64Result chosen = full != null ? full : B64Detector.decodeIfValid(str, options.getMaxCommentLength());
			if (chosen == null) {
				continue;
			}
			candidates.put(idx, chosen);
			if (full != null) {
				hasAnchor = true;
			}
		}
		if (hasAnchor) {
			field.addCodeComment(B64Result.buildIndexedComment(candidates));
		}
	}

	/** Resolves an arg to a literal string (direct ConstString or SGET to a CONSTANT_VALUE field), or null. */
	private static String extractStringFromArg(FieldNode contextField, InsnArg arg) {
		InsnNode argInsn = resolveArgInsn(arg);
		if (argInsn instanceof ConstStringNode) {
			return ((ConstStringNode) argInsn).getString();
		}
		if (argInsn != null && argInsn.getType() == InsnType.SGET) {
			FieldNode refField = resolveFieldFromSget(contextField, (IndexInsnNode) argInsn);
			return refField != null ? FieldConstants.readStringValue(refField) : null;
		}
		return null;
	}

	/**
	 * Walks the instruction arg tree looking for a ConstStringNode. When the ConstStringNode is a
	 * direct arg to a Base64.decode-like call, decodes unconditionally; otherwise applies normal
	 * false-positive checks. Returns true if a comment was added.
	 */
	private boolean findAndAnnotateInArgTree(FieldNode field, InsnNode insn, int depth) {
		if (insn == null || depth > MAX_ARG_TREE_DEPTH) {
			return false;
		}
		boolean isBase64Call = B64DecodeCalls.isDecodeCall(insn);
		for (int i = 0; i < insn.getArgsCount(); i++) {
			InsnNode argInsn = resolveArgInsn(insn.getArg(i));
			if (argInsn == null) {
				continue;
			}
			if (argInsn instanceof ConstStringNode) {
				if (annotateField(field, ((ConstStringNode) argInsn).getString(), isBase64Call)) {
					return true;
				}
			} else if (argInsn.getType() == InsnType.SGET) {
				if (annotateFromSgetReference(field, (IndexInsnNode) argInsn, isBase64Call)) {
					return true;
				}
			} else if (findAndAnnotateInArgTree(field, argInsn, depth + 1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The const-string may have been rewritten to sget by JADX's replaceConsts when a matching
	 * CONSTANT_VALUE field exists. Annotate the consuming field; if the original call was a
	 * Base64.decode, also force-annotate the referenced source field so the encoded literal is
	 * commented at its declaration site.
	 */
	private boolean annotateFromSgetReference(FieldNode consumingField, IndexInsnNode sget, boolean isBase64Call) {
		FieldNode refField = resolveFieldFromSget(consumingField, sget);
		if (refField == null) {
			return false;
		}
		String str = FieldConstants.readStringValue(refField);
		if (str == null || !annotateField(consumingField, str, isBase64Call)) {
			return false;
		}
		if (isBase64Call && refField.get(AType.FIELD_INIT_INSN) == null && !B64FalsePositives.contains(str)) {
			B64Result r = B64Detector.decodeForced(str, options.getMaxCommentLength());
			if (r != null) {
				refField.addCodeComment(r.commentText());
			}
		}
		return true;
	}

	private static FieldNode resolveFieldFromSget(FieldNode contextField, IndexInsnNode sgetInsn) {
		FieldInfo refFieldInfo = (FieldInfo) sgetInsn.getIndex();
		ClassNode declCls = contextField.root().resolveClass(refFieldInfo.getDeclClass());
		return declCls != null ? declCls.searchField(refFieldInfo) : null;
	}

	private static InsnNode resolveArgInsn(InsnArg arg) {
		if (arg instanceof InsnWrapArg) {
			return ((InsnWrapArg) arg).getWrapInsn();
		}
		if (arg instanceof RegisterArg) {
			return ((RegisterArg) arg).getAssignInsn();
		}
		return null;
	}

	private boolean annotateField(FieldNode field, String str, boolean forced) {
		if (B64FalsePositives.contains(str)) {
			return false;
		}
		B64Result result = forced
				? B64Detector.decodeForced(str, options.getMaxCommentLength())
				: B64Detector.detect(str, options);
		if (result == null) {
			return false;
		}
		field.addCodeComment(result.commentText());
		return true;
	}
}
