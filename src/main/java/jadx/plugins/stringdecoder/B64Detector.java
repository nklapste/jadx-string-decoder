package jadx.plugins.stringdecoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public final class B64Detector {

	// Allow embedded \n/\r (PEM/MIME line-wrapped Base64); = padding and trailing newline are optional
	private static final Pattern BASE64_STANDARD = Pattern.compile("^[A-Za-z0-9+/\\n\\r]*=*[\\n\\r]*$");
	private static final Pattern BASE64_URL_SAFE = Pattern.compile("^[A-Za-z0-9_\\-\\n\\r]*=*[\\n\\r]*$");
	private static final Base64.Decoder[] STANDARD_AND_URL = { Base64.getDecoder(), Base64.getUrlDecoder() };
	private static final Base64.Decoder[] ALL_DECODERS = { Base64.getDecoder(), Base64.getUrlDecoder(), Base64.getMimeDecoder() };

	private B64Detector() {
	}

	/**
	 * Returns the decoded+truncated string if {@code str} looks like intentional Base64, otherwise null.
	 * All detection thresholds are taken from {@code options}.
	 */
	public static String detect(String str, B64DeobfuscateOptions options) {
		if (B64FalsePositives.contains(str)) {
			return null;
		}
		if (str.length() < options.getMinInputLength()) {
			return null;
		}
		if (options.isRequirePadding() && !str.endsWith("=")) {
			return null;
		}
		if (!BASE64_STANDARD.matcher(str).matches() && !BASE64_URL_SAFE.matcher(str).matches()) {
			return null;
		}
		String decoded = tryDecode(str, options);
		if (decoded == null) {
			return null;
		}
		return truncate(decoded, options.getMaxCommentLength());
	}

	private static String tryDecode(String str, B64DeobfuscateOptions options) {
		String result = attemptDecode(Base64.getDecoder(), str, options);
		if (result != null) {
			return result;
		}
		result = attemptDecode(Base64.getUrlDecoder(), str, options);
		if (result != null) {
			return result;
		}
		// MIME decoder ignores embedded whitespace (PEM line-wrapped Base64)
		if (str.indexOf('\n') >= 0 || str.indexOf('\r') >= 0) {
			return attemptDecode(Base64.getMimeDecoder(), str, options);
		}
		return null;
	}

	private static String attemptDecode(Base64.Decoder decoder, String str, B64DeobfuscateOptions options) {
		try {
			byte[] bytes = decoder.decode(str);
			CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			String decoded = utf8.decode(ByteBuffer.wrap(bytes)).toString();
			if (!isPrintable(decoded, options.getMinPrintableRatio())) {
				return null;
			}
			if (!isAlphanumeric(decoded, options.getMinAlphanumericRatio())) {
				return null;
			}
			int minLen = options.getMinDecodedLength();
			if (minLen > 0 && decoded.length() < minLen) {
				return null;
			}
			return decoded;
		} catch (CharacterCodingException ignored) {
			// decoded bytes are not valid UTF-8
		} catch (Exception ignored) {
		}
		return null;
	}

	// Counts printable ASCII (32-126) plus common whitespace (\t, \n, \r) as printable.
	// Non-ASCII Unicode chars (e.g. Hebrew letters from garbage decodes) do not count.
	private static boolean isPrintable(String str, double minRatio) {
		if (str.isEmpty()) {
			return false;
		}
		long printableCount = str.chars()
				.filter(c -> (c >= 32 && c <= 126) || c == '\t' || c == '\n' || c == '\r')
				.count();
		return (double) printableCount / str.length() >= minRatio;
	}

	// Only active when minRatio > 0.
	private static boolean isAlphanumeric(String str, double minRatio) {
		if (minRatio <= 0.0) {
			return true;
		}
		if (str.isEmpty()) {
			return false;
		}
		long alnumCount = str.chars()
				.filter(Character::isLetterOrDigit)
				.count();
		return (double) alnumCount / str.length() >= minRatio;
	}

	/**
	 * Returns the decoded string if {@code str} is valid Base64 that decodes to valid UTF-8,
	 * without applying heuristic filters (no length, printable-ratio, or alphanumeric-ratio checks).
	 * Used for array-element candidates when at least one sibling passes normal detection.
	 * Returns null if the charset check fails, Base64 decode throws, or the result is not valid UTF-8.
	 */
	public static String decodeIfValid(String str, int maxCommentLength) {
		if (!BASE64_STANDARD.matcher(str).matches() && !BASE64_URL_SAFE.matcher(str).matches()) {
			return null;
		}
		for (Base64.Decoder decoder : STANDARD_AND_URL) {
			try {
				byte[] bytes = decoder.decode(str);
				CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT);
				String decoded = utf8.decode(ByteBuffer.wrap(bytes)).toString();
				return truncate(decoded, maxCommentLength);
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	/**
	 * Decodes {@code str} as Base64 without applying any false-positive heuristics.
	 * Use when the string is explicitly passed to a Base64.decode call — the call itself
	 * is strong evidence of intent. Returns null only if the string is not valid Base64.
	 * Invalid UTF-8 bytes are replaced rather than causing a rejection.
	 */
	public static String decodeForced(String str, int maxCommentLength) {
		for (Base64.Decoder decoder : ALL_DECODERS) {
			try {
				byte[] bytes = decoder.decode(str);
				CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
						.onMalformedInput(CodingErrorAction.REPLACE)
						.onUnmappableCharacter(CodingErrorAction.REPLACE);
				String decoded = utf8.decode(ByteBuffer.wrap(bytes)).toString();
				return truncate(decoded, maxCommentLength);
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	static String truncate(String str, int maxLength) {
		String safe = str.replace("\n", "\\n").replace("\r", "\\r");
		if (maxLength > 0 && safe.length() > maxLength) {
			return safe.substring(0, maxLength) + "...";
		}
		return safe;
	}
}
