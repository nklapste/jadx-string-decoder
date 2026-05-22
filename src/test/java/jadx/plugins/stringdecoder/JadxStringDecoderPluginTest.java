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
	public void constantValueFieldB64Test() throws Exception {
		// B64 string as a literal CONSTANT_VALUE on a static final field (no <clinit>).
		// The comment must appear on the line above the field declaration, not at usage sites in method bodies.
		String code = decompileSmali("b64/constant_value_field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: Hello, World!");
		int commentPos = code.indexOf("// b64:");
		int fieldPos = code.indexOf("PREFIX =");
		// Comment is before the field declaration
		assertThat(commentPos).isLessThan(fieldPos);
		// Comment does not appear again after the field declaration (i.e. not at usage sites)
		assertThat(code.indexOf("// b64:", fieldPos + 1)).isEqualTo(-1);
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
		String code = decompileSmali("b64/long_b64.smali", Map.of(opt("maxCommentLength"), "10"));
		System.out.println(code);
		assertThat(code).contains("b64: " + "A".repeat(10) + "...");
		assertThat(code).doesNotContain("A".repeat(11));
	}

	@Test
	public void unlimitedCommentLengthOptionTest() throws Exception {
		// maxCommentLength=0 should produce a comment with all 650 decoded chars, no truncation
		String code = decompileSmali("b64/long_b64.smali", Map.of(opt("maxCommentLength"), "0"));
		System.out.println(code);
		assertThat(code).contains("b64: " + "A".repeat(650));
		assertThat(code).doesNotContain("...");
	}

	@Test
	public void identifierLikeB64NotFlaggedTest() throws Exception {
		// "fillItem" matches the Base64 charset but decodes to garbage (~)e"<non-ASCII>) with only
		// 80% printable ASCII — below the default 90% threshold, so no comment should be added.
		String code = decompileSmali("b64/identifier_like_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void lowPrintableThresholdOptionTest() throws Exception {
		// Lowering minPrintablePercent to 75 and disabling skipCamelCase causes "fillItem" to be flagged
		String code = decompileSmali("b64/identifier_like_b64.smali",
				Map.of(opt("minPrintablePercent"), "75", opt("skipCamelCase"), "false"));
		System.out.println(code);
		assertThat(code).contains("b64:");
	}

	@Test
	public void requireValidLengthFiltersOddLengthTest() throws Exception {
		// "aGVsbG8" is 7 chars (7 % 4 != 0) — structurally invalid unpadded Base64; must not be flagged
		String code = decompileSmali("b64/unpadded_b64.smali",
				Map.of(opt("requireValidLength"), "yes"));
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void requireValidLengthAllowsDivisibleByFourTest() throws Exception {
		// "aGVsbG8=" is 8 chars (8 % 4 == 0) — structurally valid; must still be flagged
		String code = decompileSmali("b64/b64_decodable.smali",
				Map.of(opt("requireValidLength"), "yes"));
		System.out.println(code);
		assertThat(code).contains("b64: hello");
	}

	@Test
	public void minInputLengthOptionTest() throws Exception {
		// Raising minInputLength to 24 should suppress the 8-char "aGVsbG8=" string
		String code = decompileSmali("b64/b64_decodable.smali",
				Map.of(opt("minInputLength"), "24"));
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void minAlphanumericPercentOptionTest() throws Exception {
		// "fillItem" decodes to ~40% alphanumeric; setting minAlphanumericPercent=50 must suppress it
		// (set printable threshold low enough to isolate the alnum check)
		String code = decompileSmali("b64/identifier_like_b64.smali",
				Map.of(opt("minPrintablePercent"), "75",
						opt("minAlphanumericPercent"), "50"));
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void decodedFieldB64Test() throws Exception {
		// new String(Base64.decode("...", 0)) field init — arg tree walk should find the encoded string
		// and forced decode (bypassing heuristics) because it's an explicit Base64.decode call
		String code = decompileSmali("b64/decoded_field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64: room://cloud.tencent.com/rtc");
	}

	@Test
	public void minDecodedLengthFiltersShortDecodeTest() throws Exception {
		// "aGVsbG8=" decodes to "hello" (5 chars); minDecodedLength=10 must suppress it
		String code = decompileSmali("b64/b64_decodable.smali",
				Map.of(opt("minDecodedLength"), "10"));
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void minDecodedLengthAllowsLongDecodeTest() throws Exception {
		// "SGVsbG8sIFdvcmxkIQ==" decodes to "Hello, World!" (13 chars); minDecodedLength=10 must allow it
		String code = decompileSmali("b64/hello.smali",
				Map.of(opt("minDecodedLength"), "10"));
		System.out.println(code);
		assertThat(code).contains("b64: Hello, World!");
	}

	@Test
	public void byteArrayStringTest() throws Exception {
		// byte[] field whose bytes are ASCII "Hello, World!" — plugin should add a bytes: comment
		String code = decompileSmali("bytes/byte_array_string.smali");
		System.out.println(code);
		assertThat(code).contains("bytes: \"Hello, World!\"");
	}

	@Test
	public void multilineFieldB64Test() throws Exception {
		// PEM-style line-wrapped Base64 string passed to Base64.decode — MIME decoder required
		// Both the String CONSTANT_VALUE field and the byte[] result field should be annotated
		String code = decompileSmali("b64/multiline_field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64(mime): Hello, World!");
	}

	@Test
	public void pemB64FieldTest() throws Exception {
		// PEM Base64 String CONSTANT_VALUE + byte[] field via Base64.decode — both should get b64(mime): comment
		String code = decompileSmali("b64/pem_b64_field.smali");
		System.out.println(code);
		assertThat(code).contains("b64(mime):");
	}

	@Test
	public void antifridas8kTest() throws Exception {
		// Real-world anti-frida/xposed detection class (s8.k) with filled-new-array/range.
		// All 9 strings in the array should get indexed comments. Padded strings (frida,
		// libAndHook, liblsposed) anchor contextual decoding of the unpadded siblings that
		// would otherwise be filtered by skipIdentifiers or requirePadding. Index 2 ("Z3Vt")
		// is below minInputLength=8 individually, but the array context includes it too.
		String code = decompileSmali("b64/antifrida_s8k.smali");
		System.out.println(code);
		assertThat(code).contains("b64[0]: xposed");
		assertThat(code).contains("b64[1]: frida");
		assertThat(code).contains("b64[2]: gum");
		assertThat(code).contains("b64[3]: linjector");
		assertThat(code).contains("b64[4]: magisk");
		assertThat(code).contains("b64[5]: substrate");
		assertThat(code).contains("b64[6]: gdbserver");
		assertThat(code).contains("b64[7]: libAndHook");
		assertThat(code).contains("b64[8]: liblsposed");
	}

	@Test
	public void filledArrayB64Test() throws Exception {
		// String[] initialised with two B64 strings via filled-new-array.
		// Both decoded values should appear as indexed comments on the array instruction.
		String code = decompileSmali("b64/filled_array_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64[0]: Hello, World!");
		assertThat(code).contains("b64[1]: hello");
		assertThat(code).contains("b64[2]: foobar");
		assertThat(code).contains("b64[3]: base64");
		assertThat(code).contains("b64[4]: testing");
	}

	@Test
	public void falsePositiveSystemJobSchedulerTest() throws Exception {
		// "SystemJobScheduler" is a known Android class name that slips past ratio filters
		String code = decompileSmali("b64/false_positive_system_job_scheduler.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void urlSafeB64Test() throws Exception {
		// "SGVsbG9-" is URL-safe Base64 for "Hello~" — '-' maps to index 62 (standard '+')
		String code = decompileSmali("b64/urlsafe_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64(url): Hello~");
	}

	@Test
	public void camelCaseNotFlaggedTest() throws Exception {
		// "getContext" matches camelCase pattern — should be suppressed by default
		String code = decompileSmali("b64/camelcase_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void camelCaseSkipDisabledTest() throws Exception {
		// with skipCamelCase=false, "getContext" is no longer suppressed by the camelCase filter;
		// other filters (printable ratio, alnum ratio) will still apply
		String code = decompileSmali("b64/camelcase_b64.smali",
				Map.of(opt("skipCamelCase"), "false"));
		System.out.println(code);
		// "getContext" decodes to non-UTF-8 bytes so no comment expected even without the filter
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void byteArrayStringPassDisabledTest() throws Exception {
		// with enableByteArrayStringPass=false the bytes: comment must not appear
		String code = decompileSmali("bytes/byte_array_string.smali",
				Map.of(opt("enableByteArrayStringPass"), "false"));
		System.out.println(code);
		assertThat(code).doesNotContain("bytes:");
	}

	private String decompileSmali(String fileName) throws Exception {
		return decompileSmali(fileName, Map.of());
	}

	private static String opt(String key) {
		return JadxStringDecoderPlugin.PLUGIN_ID + "." + key;
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
