package jadx.plugins.stringdecoder;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;

import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.FieldInitInsnAttr;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class ByteArrayStringPass implements JadxDecompilePass {

	private final B64DeobfuscateOptions options;

	public ByteArrayStringPass(B64DeobfuscateOptions options) {
		this.options = options;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"ByteArrayString",
				"Detect byte array fields whose bytes form a printable string and add comment")
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
		FilledNewArrayNode arr = extractByteOrIntArray(field);
		if (arr == null) {
			return;
		}
		byte[] bytes = extractByteLiterals(arr);
		if (bytes == null) {
			return;
		}
		String decoded = decodeUtf8OrNull(bytes);
		if (decoded == null || !passesPrintabilityChecks(decoded)) {
			return;
		}
		field.addCodeComment("bytes: \"" + B64Detector.truncate(decoded, options.getMaxCommentLength()) + "\"");
	}

	/** Returns the field's filled-new-array init if its element type is byte or int, otherwise null. */
	private static FilledNewArrayNode extractByteOrIntArray(FieldNode field) {
		FieldInitInsnAttr initAttr = field.get(AType.FIELD_INIT_INSN);
		if (initAttr == null || !(initAttr.getInsn() instanceof FilledNewArrayNode)) {
			return null;
		}
		FilledNewArrayNode arr = (FilledNewArrayNode) initAttr.getInsn();
		ArgType elem = arr.getElemType();
		if (!ArgType.BYTE.equals(elem) && !ArgType.INT.equals(elem)) {
			return null;
		}
		return arr.getArgsCount() == 0 ? null : arr;
	}

	/**
	 * Converts every array element to a byte. Returns null if any element is non-literal or
	 * outside {@code [0, 255]} — the latter rules out int[] sentinel tables (e.g. {@code -1}
	 * markers in a Base64 decode table).
	 */
	private static byte[] extractByteLiterals(FilledNewArrayNode arr) {
		int count = arr.getArgsCount();
		byte[] bytes = new byte[count];
		for (int i = 0; i < count; i++) {
			InsnArg arg = arr.getArg(i);
			if (!(arg instanceof LiteralArg)) {
				return null;
			}
			long val = ((LiteralArg) arg).getLiteral();
			if (val < 0 || val > 255) {
				return null;
			}
			bytes[i] = (byte) val;
		}
		return bytes;
	}

	private static String decodeUtf8OrNull(byte[] bytes) {
		try {
			return B64Detector.decodeUtf8(bytes, CodingErrorAction.REPORT);
		} catch (CharacterCodingException ignored) {
			return null;
		}
	}

	private boolean passesPrintabilityChecks(String decoded) {
		if (!B64Detector.isPrintable(decoded, options.getByteArrayMinPrintableRatio())) {
			return false;
		}
		int minDecoded = options.getMinDecodedLength();
		return minDecoded <= 0 || decoded.length() >= minDecoded;
	}
}
