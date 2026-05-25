package jadx.plugins.stringdecoder;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ByteArrayStringPassTest extends PluginTestBase {

	@Test
	public void byteArrayStringTest() throws Exception {
		// byte[] field whose bytes are ASCII "Hello, World!" — plugin should add a bytes: comment
		String code = decompileSmali("bytes/byte_array_string.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"Hello, World!\"");
	}

	@Test
	public void intArrayUtf8Test() throws Exception {
		// int[] storing "café" as unsigned UTF-8 bytes {99, 97, 102, 195, 169}.
		// Values 195 and 169 exceed signed byte range; byte[] would need negative literals, int[] avoids that.
		String code = decompileSmali("bytes/int_array_utf8.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"café\"");
	}

	@Test
	public void twoByteArraysInClinitTest() throws Exception {
		// Two byte[] fields initialised sequentially in <clinit> via new-array + fill-array-data.
		// Mirrors the real-world Base64$Encoder pattern — both fields should get bytes: comments.
		String code = decompileSmali("bytes/two_byte_arrays.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\"");
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_\"");
	}

	@Test
	public void b64AlphabetByteAndIntArrayTest() throws Exception {
		// Same class has a byte[] standard alphabet (+/) and an int[] URL-safe alphabet (-_).
		// Verifies both array element types are annotated correctly.
		String code = decompileSmali("bytes/b64_alphabet_byte_array.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\"");
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_\"");
	}

	@Test
	public void base64EncoderInnerClassTest() throws Exception {
		// Synthetic inner class with two byte[] fields (standard and URL-safe Base64 alphabets)
		// initialised sequentially in <clinit>. Verifies ByteArrayStringPass recurses into inner classes.
		String code = decompileSmali("bytes/base64_encoder_inner_class.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\"");
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_\"");
	}

	@Test
	public void byteArrayStringPassDisabledTest() throws Exception {
		// with enableByteArrayStringPass=false the bytes: comment must not appear
		String code = decompileSmali("bytes/byte_array_string.smali",
				Map.of(opt("enableByteArrayStringPass"), "false"));
		System.out.println(code);
		assertThat(code).doesNotContain("bytes:");
	}

	@Test
	public void intArraySentinelRejectedTest() throws Exception {
		// int[] containing -1 — the sentinel value used in Base64 decode tables for invalid chars.
		// The entire array must be rejected when any element is outside [0, 255].
		String code = decompileSmali("bytes/int_array_sentinel.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("bytes:");
	}

	@Test
	public void nonPrintableByteArrayNotAnnotatedTest() throws Exception {
		// byte[] of ASCII control characters (0x01–0x08): valid UTF-8 but 0% printable.
		// ByteArrayStringPass must not annotate arrays below the default 20% printable threshold.
		String code = decompileSmali("bytes/non_printable_bytes.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("bytes:");
	}

	@Test
	public void byteArrayMinPrintablePercentOptionTest() throws Exception {
		// "café" stored as int[] bytes {99,97,102,195,169} decodes to "café".
		// Three of four Unicode chars are ASCII-printable → 75% printable ratio.
		// Default threshold (20%) passes it; raising to 80% must suppress it.
		String codeDefault = decompileSmali("bytes/int_array_utf8.smali");
		assertThat(codeDefault).contains("bytes: \"café\"");

		String codeHighThreshold = decompileSmali("bytes/int_array_utf8.smali",
				Map.of(opt("byteArrayMinPrintablePercent"), "80"));
		System.out.println(codeHighThreshold);
		assertThat(codeHighThreshold).doesNotContain("bytes:");
	}

	@Test
	public void invalidUtf8IntArrayNotAnnotatedTest() throws Exception {
		// int[] {72, 128, 111}: values all in [0,255] so extractByteLiterals accepts them,
		// but byte 0x80 (128) is a lone UTF-8 continuation byte — decodeUtf8OrNull returns null.
		// ByteArrayStringPass must not annotate this array.
		String code = decompileSmali("bytes/invalid_utf8_int_array.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("bytes:");
	}

	@Test
	public void byteArrayMaxCommentLengthTruncationTest() throws Exception {
		// 120-byte array of ASCII 'A' chars — default maxCommentLength=100 truncates the bytes: comment
		String codeDefault = decompileSmali("bytes/long_byte_array.smali");
		System.out.println(codeDefault);
		assertThat(codeDefault).contains("bytes: \"" + "A".repeat(100) + "...");
		assertThat(codeDefault).doesNotContain("A".repeat(101));

		// maxCommentLength=10 truncates to 10 chars + "..."
		String codeTruncated = decompileSmali("bytes/long_byte_array.smali",
				Map.of(opt("maxCommentLength"), "10"));
		assertThat(codeTruncated).contains("bytes: \"" + "A".repeat(10) + "...");
		assertThat(codeTruncated).doesNotContain("A".repeat(11));
	}

	@Test
	public void byteArrayMinDecodedLengthTest() throws Exception {
		// byte[] "Hi" (2 chars, 100% printable) — below the default minDecodedLength=4.
		// Default settings must suppress it; lowering minDecodedLength to 2 must annotate it.
		String codeDefault = decompileSmali("bytes/short_byte_array.smali");
		assertThat(codeDefault).doesNotContain("bytes:");

		String codeLowThreshold = decompileSmali("bytes/short_byte_array.smali",
				Map.of(opt("minDecodedLength"), "2"));
		System.out.println(codeLowThreshold);
		assertThat(codeLowThreshold).contains("bytes: \"Hi\"");
	}

}
