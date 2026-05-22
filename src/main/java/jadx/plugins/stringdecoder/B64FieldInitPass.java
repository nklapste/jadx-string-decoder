package jadx.plugins.stringdecoder;

import java.util.Locale;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.FieldInitInsnAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64FieldInitPass implements JadxDecompilePass {

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
		return false;
	}

	private void processField(FieldNode field) {
		// Case 1: field initialised via <clinit> / constructor and extracted by ExtractFieldInit
		FieldInitInsnAttr initAttr = field.get(AType.FIELD_INIT_INSN);
		if (initAttr != null) {
			InsnNode initInsn = initAttr.getInsn();
			if (initInsn instanceof ConstStringNode) {
				annotateField(field, ((ConstStringNode) initInsn).getString(), false);
			} else {
				findAndAnnotateInArgTree(field, initInsn, 0);
			}
			return;
		}

		// Case 2: static final field with a literal value encoded as CONSTANT_VALUE (no <clinit>)
		EncodedValue constVal = field.get(JadxAttrType.CONSTANT_VALUE);
		if (constVal != null && constVal.getType() == EncodedType.ENCODED_STRING) {
			Object val = constVal.getValue();
			if (val instanceof String) {
				annotateField(field, (String) val, false);
			}
		}
	}

	/**
	 * Recursively walks the instruction arg tree looking for a ConstStringNode.
	 * When the ConstStringNode is a direct arg of a Base64.decode-like call, decodes
	 * unconditionally (the call itself is strong evidence of intent).
	 * Otherwise, applies normal false-positive checks via {@link B64Detector#detect}.
	 */
	private static final int MAX_ARG_TREE_DEPTH = 8;

	private boolean findAndAnnotateInArgTree(FieldNode field, InsnNode insn, int depth) {
		if (insn == null || depth > MAX_ARG_TREE_DEPTH) {
			return false;
		}
		boolean isBase64Call = isBase64DecodeCall(insn);
		for (int i = 0; i < insn.getArgsCount(); i++) {
			InsnArg arg = insn.getArg(i);
			InsnNode argInsn = resolveArgInsn(arg);
			if (argInsn == null) {
				continue;
			}
			if (argInsn instanceof ConstStringNode) {
				String str = ((ConstStringNode) argInsn).getString();
				if (annotateField(field, str, isBase64Call)) {
					return true;
				}
			} else if (argInsn.getType() == InsnType.SGET) {
				// const-string may have been replaced by an SGET to a CONSTANT_VALUE field
				// (JADX's replaceConsts rewrites const-string to sget when a matching field exists)
				String str = resolveConstStringFromSget(field, (IndexInsnNode) argInsn);
				if (str != null) {
					if (annotateField(field, str, isBase64Call)) {
						// The string is a direct Base64.decode arg — also force-annotate the source
						// String field so it gets a comment even if it fails the alphanumeric check
						if (isBase64Call) {
							forceAnnotateSourceField(field, (IndexInsnNode) argInsn, str);
						}
						return true;
					}
				}
			} else if (findAndAnnotateInArgTree(field, argInsn, depth + 1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * When a CONSTANT_VALUE String field is the direct arg to a Base64.decode call,
	 * force-annotates it so the source field gets a comment regardless of the alphanumeric
	 * threshold (the explicit decode call is sufficient evidence of intent).
	 */
	private void forceAnnotateSourceField(FieldNode contextField, IndexInsnNode sgetInsn, String str) {
		FieldInfo refFieldInfo = (FieldInfo) sgetInsn.getIndex();
		ClassNode declCls = contextField.root().resolveClass(refFieldInfo.getDeclClass());
		if (declCls == null) {
			return;
		}
		FieldNode srcField = declCls.searchField(refFieldInfo);
		if (srcField == null || srcField.get(AType.FIELD_INIT_INSN) != null) {
			return;
		}
		String decoded = B64Detector.decodeForced(str, options.getMaxCommentLength());
		if (decoded != null) {
			srcField.addCodeComment("b64: " + decoded);
		}
	}

	/** Follows an SGET to the referenced field's CONSTANT_VALUE string, or returns null. */
	private static String resolveConstStringFromSget(FieldNode contextField, IndexInsnNode sgetInsn) {
		FieldInfo refFieldInfo = (FieldInfo) sgetInsn.getIndex();
		RootNode root = contextField.root();
		ClassNode declCls = root.resolveClass(refFieldInfo.getDeclClass());
		if (declCls == null) {
			return null;
		}
		FieldNode refField = declCls.searchField(refFieldInfo);
		if (refField == null) {
			return null;
		}
		EncodedValue constVal = refField.get(JadxAttrType.CONSTANT_VALUE);
		if (constVal != null && constVal.getType() == EncodedType.ENCODED_STRING) {
			Object val = constVal.getValue();
			if (val instanceof String) {
				return (String) val;
			}
		}
		return null;
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

	/** Returns true if {@code insn} looks like a call to a Base64 decode method. */
	static boolean isBase64DecodeCall(InsnNode insn) {
		if (!(insn instanceof InvokeNode)) {
			return false;
		}
		MethodInfo mth = ((InvokeNode) insn).getCallMth();
		String clsName = mth.getDeclClass().getFullName().toLowerCase(Locale.ROOT);
		String mthName = mth.getName().toLowerCase(Locale.ROOT);
		return clsName.contains("base64") && mthName.contains("decode");
	}

	/**
	 * Annotates the field if the string is valid Base64.
	 * When {@code forced} is true, skips false-positive heuristics.
	 * Returns true if a comment was added.
	 */
	private boolean annotateField(FieldNode field, String str, boolean forced) {
		String decoded = forced
				? B64Detector.decodeForced(str, options.getMaxCommentLength())
				: B64Detector.detect(str, options);
		if (decoded != null) {
			field.addCodeComment("b64: " + decoded);
			return true;
		}
		return false;
	}

	@Override
	public void visit(MethodNode mth) {
	}
}
