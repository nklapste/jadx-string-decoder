package jadx.plugins.stringdecoder;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JadxStringDecoderPluginTest {

	@Test
	public void integrationTest() throws Exception {
		String code = decompileSmali("b64/hello.smali");
		System.out.println(code);
		assertThat(code).contains("b64: Hello, World!");
	}

	@Test
	public void notB64Test() throws Exception {
		// "Hello, World!" contains non-Base64 chars — no comment expected
		String code = decompileSmali("b64/not_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void looksLikeB64Test() throws Exception {
		// "AQIDBAUG" matches the Base64 charset but decodes to binary [1,2,3,4,5,6] — not printable UTF-8, no comment expected
		String code = decompileSmali("b64/looks_like_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void b64DecodableNotEncodedTest() throws Exception {
		// "aGVsbG8=" is not intentionally Base64-encoded but decodes to "hello" — plugin adds a comment (known false positive)
		String code = decompileSmali("b64/b64_decodable.smali");
		System.out.println(code);
		assertThat(code).contains("b64: hello");
	}

	@Test
	public void fieldB64Test() throws Exception {
		// Base64 string assigned to a static field in <clinit>
		String code = decompileSmali("b64/field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: Hello, World!");
	}

	@Test
	public void varAssignB64Test() throws Exception {
		// Base64 string used twice — may be kept as an explicit local variable in decompiled output
		String code = decompileSmali("b64/var_assign_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: Hello, World!");
	}

	@Test
	public void longDecodedStringTest() throws Exception {
		// Decoded string is 650 'A' chars — truncated to 100 chars + "..." in the comment
		String code = decompileSmali("b64/long_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: " + "A".repeat(100) + "...");
	}

	@Test
	public void multilineDecodedStringTest() throws Exception {
		// Decoded string contains newlines — they should be escaped as \n in the comment
		String code = decompileSmali("b64/multiline_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: Line 1\\nLine 2\\nLine 3");
	}

	@Test
	public void staticFieldB64Test() throws Exception {
		// Base64 string assigned to a static field via <clinit>; comment should appear on the field declaration
		String code = decompileSmali("b64/static_field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: hello");
	}

	@Test
	public void maxCommentLengthOptionTest() throws Exception {
		// maxCommentLength=10 should truncate the 650-char decoded string to 10 chars + "..."
		String code = decompileSmali("b64/long_b64.smali", Map.of("b64-deobfuscate.maxCommentLength", "10"));
		System.out.println(code);
		assertThat(code).contains("b64: " + "A".repeat(10) + "...");
		assertThat(code).doesNotContain("A".repeat(11));
	}

	@Test
	public void unlimitedCommentLengthOptionTest() throws Exception {
		// maxCommentLength=0 should produce a comment with all 650 decoded chars, no truncation
		String code = decompileSmali("b64/long_b64.smali", Map.of("b64-deobfuscate.maxCommentLength", "0"));
		System.out.println(code);
		assertThat(code).contains("b64: " + "A".repeat(650));
		assertThat(code).doesNotContain("...");
	}

	private String decompileSmali(String fileName) throws Exception {
		return decompileSmali(fileName, Map.of());
	}

	private String decompileSmali(String fileName, Map<String, String> pluginOptions) throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleFile(fileName));
		args.setPluginOptions(pluginOptions);
		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			JavaClass cls = jadx.getClasses().get(0);
			return cls.getCode();
		}
	}

	private File getSampleFile(String fileName) throws URISyntaxException {
		URL file = getClass().getClassLoader().getResource("samples/" + fileName);
		assertThat(file).isNotNull();
		return new File(file.toURI());
	}
}
