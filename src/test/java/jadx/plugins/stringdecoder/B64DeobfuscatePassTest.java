package jadx.plugins.stringdecoder;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class B64DeobfuscatePassTest extends PluginTestBase {

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
	public void emptyStringB64ArgTest() throws Exception {
		// Empty string passed to Base64.decode — decodes to zero bytes, no comment should appear.
		// Regression: decodeForced("") previously produced "// b64: " (empty decoded value).
		String code = decompileSmali("b64/empty_string_b64_arg.smali");
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
		// Base64 string in a const-string in a regular method body (instance field via iput-object)
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
	public void minAlphanumericPercentOptionTest() throws Exception {
		// "fillItem" decodes to ~40% alphanumeric; setting minAlphanumericPercent=50 must suppress it
		// (lower printable threshold and disable skipCamelCase to isolate the alnum check)
		String code = decompileSmali("b64/identifier_like_b64.smali",
				Map.of(opt("minPrintablePercent"), "75",
						opt("minAlphanumericPercent"), "50",
						opt("skipCamelCase"), "false"));
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
	public void multilineFieldB64Test() throws Exception {
		// PEM-style line-wrapped Base64 string passed to Base64.decode — MIME decoder required
		// Both the String CONSTANT_VALUE field and the byte[] result field should be annotated
		String code = decompileSmali("b64/multiline_field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64(mime): Hello, World!");
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
	public void stringArrayFieldIndexedB64Test() throws Exception {
		// String[] field initialised via new-array + aput-object (large-array path).
		// B64FieldInitPass.findAndAnnotateFilledArray should produce indexed comments
		// for the two B64 elements; plain strings at indices 0 and 2 must not appear.
		String code = decompileSmali("b64/string_array_field_b64.smali");
		System.out.println(code);
		assertThat(code).contains("b64[1]: Hello, World!");
		assertThat(code).contains("b64[3]: hello");
		assertThat(code).doesNotContain("b64[0]:");
		assertThat(code).doesNotContain("b64[2]:");
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
	public void allCapsNotFlaggedTest() throws Exception {
		// "CURSOR" is all-uppercase and decodes to "\tDR9" — suppressed by skipSnakeCase (default true)
		String code = decompileSmali("b64/allcaps_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void allLowerNotFlaggedTest() throws Exception {
		// "closed" is all-lowercase and decodes to "rZ,y" — suppressed by skipSnakeCase (default true)
		String code = decompileSmali("b64/alllower_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void allLowerSkipDisabledTest() throws Exception {
		// With skipSnakeCase, skipDictionaryWords, and requireValidLength disabled,
		// "closed" (6 chars) passes all filters and decodes to "rZ,y" (100% printable)
		String code = decompileSmali("b64/alllower_b64.smali",
				Map.of(opt("skipSnakeCase"), "false",
						opt("skipDictionaryWords"), "false",
						opt("requireValidLength"), "false"));
		System.out.println(code);
		assertThat(code).contains("b64: rZ,y");
	}

	@Test
	public void dictionaryWordNotFlaggedTest() throws Exception {
		// "callback" is a single dictionary word and valid Base64 charset — but decodes to
		// invalid UTF-8 bytes so no comment is expected regardless of dictionary filter state
		String code = decompileSmali("b64/dict_word_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void pascalCaseNotFlaggedTest() throws Exception {
		// "FileUtil" matches PascalCase — suppressed by skipPascalCase (default true)
		String code = decompileSmali("b64/pascalcase_b64.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void pascalCaseSkipDisabledDecodesTest() throws Exception {
		// "SiteWith" is PascalCase and decodes to "J+^Z+a" (100% printable, 50% alnum).
		// With the filter on (default) it is suppressed; with it off it should be annotated.
		String codeDefault = decompileSmali("b64/pascalcase_decodable_b64.smali");
		assertThat(codeDefault).doesNotContain("b64:");

		String codeDisabled = decompileSmali("b64/pascalcase_decodable_b64.smali",
				Map.of(opt("skipPascalCase"), "false"));
		System.out.println(codeDisabled);
		assertThat(codeDisabled).contains("b64: J+^Z+a");
	}

	@Test
	public void multiB64InvokeChainTest() throws Exception {
		// Two const-strings both used as direct Base64.decode args in the same fully-inlined chain:
		//   Class.forName(new String(Base64.decode("...", 0)))
		//       .getMethod(new String(Base64.decode("...", 0)), ...)
		// Both are grouped under the top-level statement and emitted as indexed comments.
		String code = decompileSmali("b64/multi_b64_invoke.smali");
		System.out.println(code);
		assertThat(code).contains("b64[0]: android.app.ActivityThread");
		assertThat(code).contains("b64[1]: getPackageManager");
	}

	@Test
	public void b64ReflectTryCatchTest() throws Exception {
		// Multiple Base64.decode strings inside try-catch blocks with conditional branching.
		// ActivityThread+getPackageManager are in a fully-inlined chain → grouped as indexed comments.
		// getPackageInfo has blocking invokes between its constructor and use → kept as an explicit
		// variable str2, so the comment lands on str2 = new String(...) rather than on getMethod.
		String code = decompileSmali("b64/b64_reflect_trycatch.smali");
		System.out.println(code);
		assertThat(code).contains("b64: android.os.UserHandle");
		assertThat(code).contains("b64: getUserId");
		assertThat(code).contains("b64[0]: android.app.ActivityThread");
		assertThat(code).contains("b64[1]: getPackageManager");
		assertThat(code).contains("b64: getPackageInfo");
	}

	@Test
	public void b64CachedReflectionTest() throws Exception {
		// Three Base64.decode strings in a cached-reflection pattern with try-catch + conditionals.
		// ActivityThread+getPackageManager are in a fully-inlined chain → grouped as indexed comments.
		// getPackagesForUid has a blocking Object.getClass() between its constructor and getMethod,
		// but JADX still inlines the constructor, so the comment lands on the getMethod statement.
		String code = decompileSmali("b64/b64_cached_reflection.smali");
		System.out.println(code);
		assertThat(code).contains("b64[0]: android.app.ActivityThread");
		assertThat(code).contains("b64[1]: getPackageManager");
		assertThat(code).contains("b64: getPackagesForUid");
	}

	@Test
	public void b64InTryCatchTest() throws Exception {
		// new String(Base64.decode("Z2V0UGFja2FnZUluZm8=", 0)) inside a .catchall block.
		// "getPackageInfo" is camelCase and would normally be filtered, but the explicit
		// Base64.decode call triggers decodeForced() which bypasses heuristics.
		String code = decompileSmali("b64/b64_in_trycatch.smali");
		System.out.println(code);
		assertThat(code).contains("b64: getPackageInfo");
	}

	@Test
	public void b64ConditionalTryCatchTest() throws Exception {
		// Real-world pattern: cache==null branch initialises via two Base64.decode reflection
		// strings; an sdk>=33 conditional selects a delegate path; else branch uses a third
		// Base64.decode string. All three must be annotated despite the branching structure.
		String code = decompileSmali("b64/b64_conditional_trycatch.smali");
		System.out.println(code);
		assertThat(code).contains("b64[0]: android.app.ActivityThread");
		assertThat(code).contains("b64[1]: getPackageManager");
		assertThat(code).contains("b64: getPackageInfo");
	}

	@Test
	public void b64DecodePassDisabledTest() throws Exception {
		// enableB64DecodePass=false must suppress all b64: comments from the main detection pass
		String code = decompileSmali("b64/hello.smali",
				Map.of(opt("enableB64DecodePass"), "false"));
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void b64DecodePassDisabledAlsoDisablesFieldInitPassTest() throws Exception {
		// enableB64DecodePass=false gates all three passes, including B64FieldInitPass.
		// A static field initialised with a Base64 string must not be annotated.
		String code = decompileSmali("b64/static_field_b64.smali",
				Map.of(opt("enableB64DecodePass"), "false"));
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
	}

	@Test
	public void filledArrayNoAnchorNotAnnotatedTest() throws Exception {
		// String[] where every element ("aGVs") decodes to valid Base64+UTF-8 ("hel", 3 chars)
		// but fails full heuristic detection (decoded length 3 < default minDecodedLength=4).
		// hasAnchor=false so neither b64: nor b64[N]: comments are emitted.
		String code = decompileSmali("b64/filled_array_no_anchor.smali");
		System.out.println(code);
		assertThat(code).doesNotContain("b64:");
		assertThat(code).doesNotContain("b64[");
	}

}
