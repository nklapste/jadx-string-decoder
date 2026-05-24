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
	public void byteArrayB64AlphabetTest() throws Exception {
		// byte[] alphabet field — standard Base64 (+/)
		String code = decompileSmali("bytes/b64_alphabet_byte_array.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\"");
	}

	@Test
	public void intArrayB64AlphabetTest() throws Exception {
		// int[] alphabet field — URL-safe Base64 (-_); verifies int[] is treated identically to byte[]
		String code = decompileSmali("bytes/b64_alphabet_byte_array.smali");
		System.out.println(code);
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

}
