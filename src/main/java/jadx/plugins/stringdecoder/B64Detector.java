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
	// camelCase: starts with lowercase run, then one or more UppercaseLowercase+ groups, no digits/symbols
	private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z]+([A-Z][a-z]+)+$");
	// PascalCase: starts with exactly one uppercase, then a lowercase run, then more UpperLower groups
	private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-z]+([A-Z][a-z0-9]+)+$");
	// all-uppercase (letters, digits, hyphens, underscores) — covers CURSOR, UTF-16BE, FOO_BAR, SETTING
	private static final Pattern ALL_CAPS = Pattern.compile("^[A-Z][A-Z0-9\\-_]*$");
	// all-lowercase (letters, digits, underscores) — covers foo_bar, closed, callback, binding
	private static final Pattern ALL_LOWER = Pattern.compile("^[a-z][a-z0-9_]*$");
	private static final Base64.Decoder[] STANDARD_AND_URL = { Base64.getDecoder(), Base64.getUrlDecoder() };
	private static final Base64.Decoder[] ALL_DECODERS = { Base64.getDecoder(), Base64.getUrlDecoder(), Base64.getMimeDecoder() };
	private static final String[] STANDARD_AND_URL_TAGS = { "", "url" };
	private static final String[] ALL_DECODER_TAGS = { "", "url", "mime" };

	private B64Detector() {
	}

	/**
	 * Returns a {@link B64Result} if {@code str} looks like intentional Base64, otherwise null.
	 * All detection thresholds are taken from {@code options}.
	 */
	public static B64Result detect(String str, B64DeobfuscateOptions options) {
		if (B64FalsePositives.contains(str)) {
			return null;
		}
		if (options.isRequireValidLength() && str.length() % 4 != 0) {
			return null;
		}
		if (options.isSkipCamelCase() && str.length() < 40 && CAMEL_CASE.matcher(str).matches()) {
			return null;
		}
		if (options.isSkipPascalCase() && str.length() < 40 && PASCAL_CASE.matcher(str).matches()) {
			return null;
		}
		if (options.isSkipSnakeCase() && str.length() < 40
				&& (ALL_CAPS.matcher(str).matches() || ALL_LOWER.matcher(str).matches())) {
			return null;
		}
		if (options.isSkipDictionaryWords() && str.length() < 40 && B64DictionaryFilter.isAllDictionaryWords(str)) {
			return null;
		}
		if (!BASE64_STANDARD.matcher(str).matches() && !BASE64_URL_SAFE.matcher(str).matches()) {
			return null;
		}
		return tryDecode(str, options);
	}

	private static B64Result tryDecode(String str, B64DeobfuscateOptions options) {
		boolean hasLineBreaks = str.indexOf('\n') >= 0 || str.indexOf('\r') >= 0;
		Base64.Decoder[] decoders = hasLineBreaks ? ALL_DECODERS : STANDARD_AND_URL;
		String[] tags = hasLineBreaks ? ALL_DECODER_TAGS : STANDARD_AND_URL_TAGS;
		for (int i = 0; i < decoders.length; i++) {
			B64Result result = attemptDecode(decoders[i], str, options, tags[i]);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private static CharsetDecoder newUtf8Decoder(CodingErrorAction action) {
		return StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(action)
				.onUnmappableCharacter(action);
	}

	private static B64Result attemptDecode(Base64.Decoder decoder, String str, B64DeobfuscateOptions options, String tag) {
		try {
			byte[] bytes = decoder.decode(str);
			String decoded = newUtf8Decoder(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
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
			return new B64Result(truncate(decoded, options.getMaxCommentLength()), tag);
		} catch (CharacterCodingException ignored) {
			// decoded bytes are not valid UTF-8
		} catch (IllegalArgumentException ignored) {
			// decoder.decode() rejects malformed Base64
		}
		return null;
	}

	// Counts printable ASCII (32-126) plus common whitespace (\t, \n, \r) as printable.
	// Non-ASCII Unicode chars (e.g. Hebrew letters from garbage decodes) do not count.
	static boolean isPrintable(String str, double minRatio) {
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
	 * Returns a {@link B64Result} if {@code str} is valid Base64 that decodes to valid UTF-8,
	 * without applying heuristic filters (no length, printable-ratio, or alphanumeric-ratio checks).
	 * Used for array-element candidates when at least one sibling passes normal detection.
	 * Returns null if the charset check fails, Base64 decode throws, or the result is not valid UTF-8.
	 */
	public static B64Result decodeIfValid(String str, int maxCommentLength) {
		if (!BASE64_STANDARD.matcher(str).matches() && !BASE64_URL_SAFE.matcher(str).matches()) {
			return null;
		}
		boolean hasLineBreaks = str.indexOf('\n') >= 0 || str.indexOf('\r') >= 0;
		Base64.Decoder[] decoders = hasLineBreaks ? ALL_DECODERS : STANDARD_AND_URL;
		String[] tags = hasLineBreaks ? ALL_DECODER_TAGS : STANDARD_AND_URL_TAGS;
		for (int i = 0; i < decoders.length; i++) {
			try {
				byte[] bytes = decoders[i].decode(str);
				if (bytes.length == 0) {
					return null;
				}
					String decoded = newUtf8Decoder(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
				return new B64Result(truncate(decoded, maxCommentLength), tags[i]);
			} catch (IllegalArgumentException | CharacterCodingException ignored) {
				// decoder.decode() rejects malformed Base64; utf8.decode() rejects invalid UTF-8
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
	public static B64Result decodeForced(String str, int maxCommentLength) {
		for (int i = 0; i < ALL_DECODERS.length; i++) {
			try {
				byte[] bytes = ALL_DECODERS[i].decode(str);
				if (bytes.length == 0) {
					return null;
				}
				String decoded = newUtf8Decoder(CodingErrorAction.REPLACE).decode(ByteBuffer.wrap(bytes)).toString();
				return new B64Result(truncate(decoded, maxCommentLength), ALL_DECODER_TAGS[i]);
			} catch (IllegalArgumentException | CharacterCodingException ignored) {
				// decoder.decode() rejects malformed Base64; utf8.decode() on REPLACE mode won't throw, but guard anyway
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
