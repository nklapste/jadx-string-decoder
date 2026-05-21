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
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ConstStringNode;
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
	private boolean findAndAnnotateInArgTree(FieldNode field, InsnNode insn, int depth) {
		if (insn == null || depth > 8) {
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
			} else if (findAndAnnotateInArgTree(field, argInsn, depth + 1)) {
				return true;
			}
		}
		return false;
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
