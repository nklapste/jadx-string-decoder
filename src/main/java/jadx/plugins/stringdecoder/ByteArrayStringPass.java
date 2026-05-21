package jadx.plugins.stringdecoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

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
import jadx.core.dex.nodes.InsnNode;
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
		return false;
	}

	private void processField(FieldNode field) {
		FieldInitInsnAttr initAttr = field.get(AType.FIELD_INIT_INSN);
		if (initAttr == null) {
			return;
		}
		InsnNode insn = initAttr.getInsn();
		if (!(insn instanceof FilledNewArrayNode)) {
			return;
		}
		FilledNewArrayNode filledArr = (FilledNewArrayNode) insn;
		if (!ArgType.BYTE.equals(filledArr.getElemType())) {
			return;
		}
		byte[] bytes = extractLiteralBytes(filledArr);
		if (bytes == null || bytes.length < Math.max(1, options.getMinInputLength())) {
			return;
		}
		String decoded = tryDecodeAsString(bytes);
		if (decoded != null) {
			field.addCodeComment("bytes: \"" + B64Detector.truncate(decoded, options.getMaxCommentLength()) + "\"");
		}
	}

	private static byte[] extractLiteralBytes(FilledNewArrayNode insn) {
		int count = insn.getArgsCount();
		byte[] bytes = new byte[count];
		for (int i = 0; i < count; i++) {
			InsnArg arg = insn.getArg(i);
			if (!(arg instanceof LiteralArg)) {
				// Non-literal arg (e.g. SGET to a named constant) — skip this array
				return null;
			}
			bytes[i] = (byte) ((LiteralArg) arg).getLiteral();
		}
		return bytes;
	}

	private String tryDecodeAsString(byte[] bytes) {
		try {
			CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			String decoded = utf8.decode(ByteBuffer.wrap(bytes)).toString();
			if (decoded.isEmpty()) {
				return null;
			}
			long printableCount = decoded.chars()
					.filter(c -> (c >= 32 && c <= 126) || c == '\t' || c == '\n' || c == '\r')
					.count();
			if ((double) printableCount / decoded.length() < options.getMinPrintableRatio()) {
				return null;
			}
			int minDecoded = options.getMinDecodedLength();
			if (minDecoded > 0 && decoded.length() < minDecoded) {
				return null;
			}
			return decoded;
		} catch (CharacterCodingException e) {
			return null;
		}
	}

	@Override
	public void visit(MethodNode mth) {
	}
}
