package jadx.plugins.stringdecoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public final class B64Detector {

	private static final Pattern BASE64_STANDARD = Pattern.compile("^[A-Za-z0-9+/]+=*$");
	private static final Pattern BASE64_URL_SAFE = Pattern.compile("^[A-Za-z0-9_-]+=*$");
	private static final int MIN_LENGTH = 8;
	private static final double MIN_PRINTABLE_RATIO = 0.75;
	static final int MAX_COMMENT_LENGTH = 100;

	private B64Detector() {
	}

	/** Returns the decoded+truncated string if {@code str} looks like Base64, otherwise null. */
	public static String detect(String str) {
		if (str.length() < MIN_LENGTH) {
			return null;
		}
		if (!BASE64_STANDARD.matcher(str).matches() && !BASE64_URL_SAFE.matcher(str).matches()) {
			return null;
		}
		String decoded = tryDecode(str);
		if (decoded == null) {
			return null;
		}
		return truncate(decoded);
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

	static String truncate(String str) {
		String safe = str.replace("\n", "\\n").replace("\r", "\\r");
		if (safe.length() > MAX_COMMENT_LENGTH) {
			return safe.substring(0, MAX_COMMENT_LENGTH) + "...";
		}
		return safe;
	}
}
