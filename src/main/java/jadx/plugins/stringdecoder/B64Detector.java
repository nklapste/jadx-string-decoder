package jadx.plugins.stringdecoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Pure detection logic. No JADX types: every input is a {@link String} and every output is a
 * {@link B64Result}, so this class can be unit-tested in isolation and re-used by future encoders.
 *
 * <p>Detection runs as an ordered pipeline of small predicates ({@link InputFilter}) followed by
 * an attempt to decode against an ordered list of {@link Variant}s. Both lists are explicit, so a
 * new heuristic is added by appending one {@link InputFilter} entry rather than editing a long
 * boolean chain.
 */
public final class B64Detector {

	// Allow embedded \n/\r (PEM/MIME line-wrapped Base64); = padding and trailing newline are optional.
	private static final Pattern BASE64_STANDARD = Pattern.compile("^[A-Za-z0-9+/\\n\\r]*=*[\\n\\r]*$");
	private static final Pattern BASE64_URL_SAFE = Pattern.compile("^[A-Za-z0-9_\\-\\n\\r]*=*[\\n\\r]*$");
	// Identifier shapes — only checked below IDENTIFIER_FILTER_MAX_LEN.
	private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z]+([A-Z][a-z]+)+$");
	private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-z]+([A-Z][a-z0-9]+)+$");
	private static final Pattern ALL_CAPS = Pattern.compile("^[A-Z][A-Z0-9\\-_]*$");
	private static final Pattern ALL_LOWER = Pattern.compile("^[a-z][a-z0-9_]*$");

	private static final int IDENTIFIER_FILTER_MAX_LEN = 40;

	/** A Base64 decoder paired with the tag used in the rendered comment ("" = standard). */
	private enum Variant {
		STANDARD(Base64.getDecoder(), ""),
		URL(Base64.getUrlDecoder(), "url"),
		MIME(Base64.getMimeDecoder(), "mime");

		final Base64.Decoder decoder;
		final String tag;

		Variant(Base64.Decoder decoder, String tag) {
			this.decoder = decoder;
			this.tag = tag;
		}
	}

	/** A single pre-decode heuristic: returns true if the input should be rejected outright. */
	@FunctionalInterface
	private interface InputFilter {
		boolean rejects(String input, B64DeobfuscateOptions options);
	}

	/**
	 * Ordered list of pre-decode filters. Cheapest first; any returning true short-circuits.
	 * To add a new heuristic, append a lambda here — no other code needs to change.
	 */
	private static final InputFilter[] INPUT_FILTERS = {
			(s, o) -> B64FalsePositives.contains(s),
			(s, o) -> o.isRequireValidLength() && s.length() % 4 != 0,
			(s, o) -> isLikelyIdentifier(s, o),
			(s, o) -> !hasValidBase64Charset(s)
	};

	private B64Detector() {
	}

	/** Returns a {@link B64Result} if {@code str} looks like intentional Base64, otherwise null. */
	public static B64Result detect(String str, B64DeobfuscateOptions options) {
		if (rejectedByAnyFilter(str, options)) {
			return null;
		}
		for (Variant v : variantsFor(str)) {
			B64Result r = decodeAndValidate(v, str, options);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	/** Valid Base64 + valid UTF-8, no heuristic filters. Used for array-element candidates. */
	public static B64Result decodeIfValid(String str, int maxCommentLength) {
		if (!hasValidBase64Charset(str)) {
			return null;
		}
		return decodeWithVariants(variantsFor(str), str, maxCommentLength, CodingErrorAction.REPORT);
	}

	/** Decodes without any heuristics; replaces invalid UTF-8 instead of rejecting. */
	public static B64Result decodeForced(String str, int maxCommentLength) {
		return decodeWithVariants(Variant.values(), str, maxCommentLength, CodingErrorAction.REPLACE);
	}

	private static boolean rejectedByAnyFilter(String str, B64DeobfuscateOptions options) {
		for (InputFilter filter : INPUT_FILTERS) {
			if (filter.rejects(str, options)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLikelyIdentifier(String str, B64DeobfuscateOptions options) {
		if (str.length() >= IDENTIFIER_FILTER_MAX_LEN) {
			return false;
		}
		return (options.isSkipCamelCase() && CAMEL_CASE.matcher(str).matches())
				|| (options.isSkipPascalCase() && PASCAL_CASE.matcher(str).matches())
				|| (options.isSkipSnakeCase()
						&& (ALL_CAPS.matcher(str).matches() || ALL_LOWER.matcher(str).matches()))
				|| (options.isSkipDictionaryWords() && B64DictionaryFilter.isAllDictionaryWords(str));
	}

	private static boolean hasValidBase64Charset(String str) {
		return BASE64_STANDARD.matcher(str).matches() || BASE64_URL_SAFE.matcher(str).matches();
	}

	private static Variant[] variantsFor(String str) {
		boolean hasLineBreaks = str.indexOf('\n') >= 0 || str.indexOf('\r') >= 0;
		return hasLineBreaks ? Variant.values() : new Variant[] { Variant.STANDARD, Variant.URL };
	}

	/** Decode with a single variant and apply the post-decode heuristics. */
	private static B64Result decodeAndValidate(Variant v, String str, B64DeobfuscateOptions options) {
		try {
			byte[] bytes = v.decoder.decode(str);
			String decoded = decodeUtf8(bytes, CodingErrorAction.REPORT);
			if (!passesDecodedHeuristics(decoded, options)) {
				return null;
			}
			return new B64Result(truncate(decoded, options.getMaxCommentLength()), v.tag);
		} catch (CharacterCodingException | IllegalArgumentException ignored) {
			return null;
		}
	}

	private static boolean passesDecodedHeuristics(String decoded, B64DeobfuscateOptions options) {
		if (!isPrintable(decoded, options.getMinPrintableRatio())) {
			return false;
		}
		if (!isAlphanumeric(decoded, options.getMinAlphanumericRatio())) {
			return false;
		}
		int minLen = options.getMinDecodedLength();
		return minLen <= 0 || decoded.length() >= minLen;
	}

	private static B64Result decodeWithVariants(Variant[] variants, String str, int maxCommentLength,
			CodingErrorAction utf8ErrorAction) {
		for (Variant v : variants) {
			try {
				byte[] bytes = v.decoder.decode(str);
				if (bytes.length == 0) {
					return null;
				}
				return new B64Result(truncate(decodeUtf8(bytes, utf8ErrorAction), maxCommentLength), v.tag);
			} catch (IllegalArgumentException | CharacterCodingException ignored) {
				// try next variant
			}
		}
		return null;
	}

	static String decodeUtf8(byte[] bytes, CodingErrorAction action) throws CharacterCodingException {
		return newUtf8Decoder(action).decode(ByteBuffer.wrap(bytes)).toString();
	}

	static CharsetDecoder newUtf8Decoder(CodingErrorAction action) {
		return StandardCharsets.UTF_8.newDecoder().onMalformedInput(action).onUnmappableCharacter(action);
	}

	// Printable = ASCII 32-126, plus \t \n \r. Non-ASCII Unicode (e.g. Hebrew from garbage decodes) does not count.
	static boolean isPrintable(String str, double minRatio) {
		if (str.isEmpty()) {
			return false;
		}
		long printable = str.chars()
				.filter(c -> (c >= 32 && c <= 126) || c == '\t' || c == '\n' || c == '\r')
				.count();
		return (double) printable / str.length() >= minRatio;
	}

	private static boolean isAlphanumeric(String str, double minRatio) {
		if (minRatio <= 0.0) {
			return true;
		}
		if (str.isEmpty()) {
			return false;
		}
		long alnum = str.chars().filter(Character::isLetterOrDigit).count();
		return (double) alnum / str.length() >= minRatio;
	}

	static String truncate(String str, int maxLength) {
		String safe = str.replace("\n", "\\n").replace("\r", "\\r");
		return (maxLength > 0 && safe.length() > maxLength) ? safe.substring(0, maxLength) + "..." : safe;
	}
}
