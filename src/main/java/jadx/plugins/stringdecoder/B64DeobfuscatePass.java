package jadx.plugins.stringdecoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import jadx.api.data.CommentStyle;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.codegen.utils.CodeComment;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class B64DeobfuscatePass implements JadxDecompilePass {

	private static final Pattern BASE64_STANDARD = Pattern.compile("^[A-Za-z0-9+/]+=*$");
	private static final Pattern BASE64_URL_SAFE = Pattern.compile("^[A-Za-z0-9_-]+=*$");
	private static final int MIN_LENGTH = 8;
	private static final double MIN_PRINTABLE_RATIO = 0.75;
	private static final int MAX_COMMENT_LENGTH = 100;

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"B64Deobfuscate",
				"Detect and decode likely Base64-encoded string constants")
				.after("SSATransform")
				.before("ConstInlineVisitor");
	}

	@Override
	public void init(RootNode root) {
	}

	@Override
	public boolean visit(ClassNode cls) {
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		if (blocks == null) {
			return;
		}
		for (BlockNode block : blocks) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn instanceof ConstStringNode) {
					processConstString((ConstStringNode) insn);
				}
			}
		}
	}

	private static void processConstString(ConstStringNode insn) {
		String str = insn.getString();
		if (str.length() < MIN_LENGTH) {
			return;
		}
		if (!BASE64_STANDARD.matcher(str).matches() && !BASE64_URL_SAFE.matcher(str).matches()) {
			return;
		}
		String decoded = tryDecode(str);
		if (decoded != null) {
			insn.addAttr(AType.CODE_COMMENTS, new CodeComment("b64: " + truncate(decoded), CommentStyle.LINE));
		}
	}

	private static String tryDecode(String str) {
		String result = attemptDecode(Base64.getDecoder(), str);
		if (result != null) {
			return result;
		}
		return attemptDecode(Base64.getUrlDecoder(), str);
	}

	private static String attemptDecode(Base64.Decoder decoder, String str) {
		try {
			byte[] bytes = decoder.decode(str);
			CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			String decoded = utf8.decode(ByteBuffer.wrap(bytes)).toString();
			if (isPrintable(decoded)) {
				return decoded;
			}
		} catch (CharacterCodingException ignored) {
			// decoded bytes are not valid UTF-8
		} catch (Exception ignored) {
		}
		return null;
	}

	private static boolean isPrintable(String str) {
		if (str.isEmpty()) {
			return false;
		}
		long printableCount = str.chars()
				.filter(c -> c >= 32 && c <= 126)
				.count();
		return (double) printableCount / str.length() >= MIN_PRINTABLE_RATIO;
	}

	private static String truncate(String str) {
		String safe = str.replace("\n", "\\n").replace("\r", "\\r");
		if (safe.length() > MAX_COMMENT_LENGTH) {
			return safe.substring(0, MAX_COMMENT_LENGTH) + "...";
		}
		return safe;
	}
}
