package jadx.plugins.stringdecoder;

import org.junit.jupiter.api.Test;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for B64Detector pure functions. No JADX pipeline — these run in milliseconds
 * and pin exact edge-case behaviour that integration tests are too coarse to isolate.
 */
class B64DetectorTest {

	// ---- helpers ----

	private static B64DeobfuscateOptions defaultOptions() {
		B64DeobfuscateOptions opts = new B64DeobfuscateOptions();
		opts.setOptions(Map.of());
		return opts;
	}

	private static B64DeobfuscateOptions options(String key, String value) {
		B64DeobfuscateOptions opts = new B64DeobfuscateOptions();
		opts.setOptions(Map.of(JadxStringDecoderPlugin.PLUGIN_ID + "." + key, value));
		return opts;
	}

	// ---- isPrintable ----

	@Test
	public void isPrintableEmptyStringReturnsFalseTest() {
		assertThat(B64Detector.isPrintable("", 0.0)).isFalse();
	}

	@Test
	public void isPrintableAllPrintableTest() {
		// All ASCII 32-126 chars → 100% printable
		assertThat(B64Detector.isPrintable("Hello, World!", 0.9)).isTrue();
	}

	@Test
	public void isPrintableControlCharsTest() {
		// Control chars 0x01-0x08 → 0% printable, fails any threshold > 0
		assertThat(B64Detector.isPrintable("", 0.2)).isFalse();
	}

	@Test
	public void isPrintableTabNewlineCrCountAsPrintableTest() {
		// \t \n \r are explicitly included in the printable set
		assertThat(B64Detector.isPrintable("\t\n\r", 1.0)).isTrue();
	}

	@Test
	public void isPrintableNonAsciiUnicodeDoesNotCountTest() {
		// 'é' (U+00E9 = 233 > 126) is NOT counted as printable — only ASCII 32-126 qualifies.
		// "café": c, a, f are printable; é is not → 3/4 = 75% printable.
		assertThat(B64Detector.isPrintable("café", 0.75)).isTrue();
		assertThat(B64Detector.isPrintable("café", 0.76)).isFalse();
	}

	// ---- truncate ----

	@Test
	public void truncateUnderLimitNoChangeTest() {
		assertThat(B64Detector.truncate("hello", 10)).isEqualTo("hello");
	}

	@Test
	public void truncateExactLimitNoChangeTest() {
		// String whose length equals maxLength is not truncated
		assertThat(B64Detector.truncate("hi", 2)).isEqualTo("hi");
	}

	@Test
	public void truncateOverLimitAppendsEllipsisTest() {
		assertThat(B64Detector.truncate("hello world", 5)).isEqualTo("hello...");
	}

	@Test
	public void truncateZeroLimitUnlimitedTest() {
		// maxLength=0 means no truncation regardless of length
		assertThat(B64Detector.truncate("A".repeat(200), 0)).isEqualTo("A".repeat(200));
	}

	@Test
	public void truncateEscapesNewlinesAndCRTest() {
		// \n and \r in decoded content must be escaped so they don't break the single-line comment
		assertThat(B64Detector.truncate("line1\nline2\rline3", 100))
				.isEqualTo("line1\\nline2\\rline3");
	}

	// ---- decodeUtf8 ----

	@Test
	public void decodeUtf8ValidBytesTest() throws CharacterCodingException {
		byte[] bytes = "Hello".getBytes(StandardCharsets.UTF_8);
		assertThat(B64Detector.decodeUtf8(bytes, CodingErrorAction.REPORT)).isEqualTo("Hello");
	}

	@Test
	public void decodeUtf8InvalidBytesWithReportThrowsTest() {
		// 0x80 is a lone UTF-8 continuation byte — REPORT mode must throw
		assertThatThrownBy(() -> B64Detector.decodeUtf8(new byte[]{(byte) 0x80}, CodingErrorAction.REPORT))
				.isInstanceOf(CharacterCodingException.class);
	}

	@Test
	public void decodeUtf8InvalidBytesWithReplaceTest() throws CharacterCodingException {
		// REPLACE mode substitutes U+FFFD instead of throwing
		String result = B64Detector.decodeUtf8(new byte[]{(byte) 0x80}, CodingErrorAction.REPLACE);
		assertThat(result).isEqualTo("�");
	}

	// ---- decodeForced ----

	@Test
	public void decodeForcedStandardB64Test() {
		B64Result r = B64Detector.decodeForced("SGVsbG8sIFdvcmxkIQ==", 100);
		assertThat(r).isNotNull();
		assertThat(r.commentText()).isEqualTo("b64: Hello, World!");
	}

	@Test
	public void decodeForcedUrlSafeB64Test() {
		// "SGVsbG9-" is URL-safe Base64 for "Hello~" (standard '+'/'/'' replaced with '-'/'_')
		B64Result r = B64Detector.decodeForced("SGVsbG9-", 100);
		assertThat(r).isNotNull();
		assertThat(r.commentText()).isEqualTo("b64(url): Hello~");
	}

	@Test
	public void decodeForcedEmptyStringReturnsNullTest() {
		// Empty string decodes to zero bytes — treated as no result
		assertThat(B64Detector.decodeForced("", 100)).isNull();
	}

	@Test
	public void decodeForcedInvalidUtf8ReturnsReplacementTest() {
		// "gA==" decodes to [0x80] (lone continuation byte) — REPLACE mode returns U+FFFD
		B64Result r = B64Detector.decodeForced("gA==", 100);
		assertThat(r).isNotNull();
		assertThat(r.getDecoded()).isEqualTo("�");
	}

	// ---- decodeIfValid ----

	@Test
	public void decodeIfValidStandardB64Test() {
		B64Result r = B64Detector.decodeIfValid("SGVsbG8sIFdvcmxkIQ==", 100);
		assertThat(r).isNotNull();
		assertThat(r.getDecoded()).isEqualTo("Hello, World!");
	}

	@Test
	public void decodeIfValidUrlSafeB64Test() {
		B64Result r = B64Detector.decodeIfValid("SGVsbG9-", 100);
		assertThat(r).isNotNull();
		assertThat(r.commentText()).isEqualTo("b64(url): Hello~");
	}

	@Test
	public void decodeIfValidInvalidCharsetReturnsNullTest() {
		// '!' is not in the Base64 charset
		assertThat(B64Detector.decodeIfValid("Hello, World!", 100)).isNull();
	}

	@Test
	public void decodeIfValidInvalidUtf8ReturnsNullTest() {
		// "gA==" decodes to [0x80] — REPORT mode (unlike decodeForced) returns null for invalid UTF-8
		assertThat(B64Detector.decodeIfValid("gA==", 100)).isNull();
	}

	@Test
	public void decodeIfValidEmptyStringReturnsNullTest() {
		// Empty string decodes to zero bytes — treated as no result
		assertThat(B64Detector.decodeIfValid("", 100)).isNull();
	}

	// ---- detect ----

	@Test
	public void detectValidStandardB64Test() {
		B64Result r = B64Detector.detect("SGVsbG8sIFdvcmxkIQ==", defaultOptions());
		assertThat(r).isNotNull();
		assertThat(r.commentText()).isEqualTo("b64: Hello, World!");
	}

	@Test
	public void detectUrlSafeB64Test() {
		// "SGVsbG9-" — '-' char routes through URL decoder, comment uses (url) tag
		B64Result r = B64Detector.detect("SGVsbG9-", defaultOptions());
		assertThat(r).isNotNull();
		assertThat(r.commentText()).isEqualTo("b64(url): Hello~");
	}

	@Test
	public void detectMimeB64WithNewlineTest() {
		// Newlines route through MIME decoder — standard/URL decoders reject \n characters.
		// Three newlines added mid-string so total length (24) stays divisible by 4,
		// satisfying the default requireValidLength=true filter.
		B64Result r = B64Detector.detect("SGVsbG8s\n\n\nIFdvcmxkIQ==\n", defaultOptions());
		assertThat(r).isNotNull();
		assertThat(r.commentText()).isEqualTo("b64(mime): Hello, World!");
	}

	@Test
	public void detectInvalidCharsetReturnsNullTest() {
		assertThat(B64Detector.detect("Hello, World!", defaultOptions())).isNull();
	}

	@Test
	public void detectBlocklistedReturnsNullTest() {
		// "SystemJobScheduler" is in B64FalsePositives — rejected before any decoding
		assertThat(B64Detector.detect("SystemJobScheduler", defaultOptions())).isNull();
	}

	@Test
	public void detectRequireValidLengthFilterTest() {
		// "aGVsbG8" is 7 chars (7%4=3) — default requireValidLength=true blocks it;
		// disabling requireValidLength allows it through
		assertThat(B64Detector.detect("aGVsbG8", defaultOptions())).isNull();
		B64Result r = B64Detector.detect("aGVsbG8", options("requireValidLength", "false"));
		assertThat(r).isNotNull();
		assertThat(r.getDecoded()).isEqualTo("hello");
	}

	@Test
	public void detectMinDecodedLengthFilterTest() {
		// "aGVs" decodes to "hel" (3 chars) — below default minDecodedLength=4
		assertThat(B64Detector.detect("aGVs", defaultOptions())).isNull();
		// Lowering the threshold to 2 allows it through
		B64Result r = B64Detector.detect("aGVs", options("minDecodedLength", "2"));
		assertThat(r).isNotNull();
		assertThat(r.getDecoded()).isEqualTo("hel");
	}

	@Test
	public void detectLowPrintableReturnsNullTest() {
		// "AQIDBAUG" decodes to bytes [1,2,3,4,5,6] — 0% printable, fails minPrintablePercent=90
		assertThat(B64Detector.detect("AQIDBAUG", defaultOptions())).isNull();
	}

	@Test
	public void detectMinAlphanumericPercentFilterTest() {
		// "ISIjJA==" decodes to "!\"#$" — 100% printable ASCII but 0% alphanumeric.
		// Default minAlphanumericPercent=35 blocks it; setting it to 0 allows it through.
		assertThat(B64Detector.detect("ISIjJA==", defaultOptions())).isNull();
		B64Result r = B64Detector.detect("ISIjJA==", options("minAlphanumericPercent", "0"));
		assertThat(r).isNotNull();
		assertThat(r.getDecoded()).isEqualTo("!\"#$");
	}
}
