package jadx.plugins.stringdecoder;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class B64DeobfuscateOptions extends BasePluginOptionsBuilder {

	private boolean enable;
	private boolean enableByteArrayStringPass;
	private int byteArrayMinPrintablePercent;
	private int minDecodedLength;
	private int maxCommentLength;
	private int minPrintablePercent;
	private int minAlphanumericPercent;
	private boolean requireValidLength;
	private boolean skipCamelCase;
	private boolean skipPascalCase;
	private boolean skipSnakeCase;
	private boolean skipDictionaryWords;

	@Override
	public void registerOptions() {
		// General options for all passes
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".maxCommentLength")
				.description("Maximum decoded string length in comment before truncating with '...' (0 for unlimited)")
				.defaultValue(100)
				.setter(v -> maxCommentLength = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minDecodedLength")
				.description("Minimum decoded string length to add a comment (0 = disabled); helps reject short garbage decodes")
				.defaultValue(4)
				.setter(v -> minDecodedLength = v);
		// B64DeobfuscatePass options
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".enableB64DecodePass")
				.description("Enable Base64 string detection and decoding")
				.defaultValue(true)
				.setter(v -> enable = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minPrintablePercent")
				.description("Minimum percentage of printable ASCII chars in decoded string (0-100); higher values reduce false positives")
				.defaultValue(90)
				.setter(v -> minPrintablePercent = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minAlphanumericPercent")
				.description("Minimum percentage of alphanumeric chars in decoded string (0-100); helps reject symbol-heavy garbage decodes")
				.defaultValue(35)
				.setter(v -> minAlphanumericPercent = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".requireValidLength")
				.description("Only flag strings whose total length is divisible by 4; rejects structurally invalid Base64")
				.defaultValue(true)
				.setter(v -> requireValidLength = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".skipCamelCase")
				.description("Skip short strings that look like camelCase identifiers (e.g. getContext, fillItem)")
				.defaultValue(true)
				.setter(v -> skipCamelCase = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".skipPascalCase")
				.description("Skip short strings that look like PascalCase type names (e.g. FileUtil, SecureRandom)")
				.defaultValue(true)
				.setter(v -> skipPascalCase = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".skipSnakeCase")
				.description("Skip short strings that are all-uppercase (CURSOR, FOO_BAR) or all-lowercase (closed, foo_bar)")
				.defaultValue(true)
				.setter(v -> skipSnakeCase = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".skipDictionaryWords")
				.description("Skip strings whose segments (split on camelCase/underscore boundaries) are all common programming/English words")
				.defaultValue(true)
				.setter(v -> skipDictionaryWords = v);

		// ByteArrayStringPass options
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".enableByteArrayStringPass")
				.description("Enable byte[] field string detection pass")
				.defaultValue(true)
				.setter(v -> enableByteArrayStringPass = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".byteArrayMinPrintablePercent")
				.description("Minimum percentage of printable ASCII chars in a byte[] field for it to be annotated (0-100)")
				.defaultValue(20)
				.setter(v -> byteArrayMinPrintablePercent = v);
	}

	public boolean isEnable() {
		return enable;
	}

	public boolean isEnableByteArrayStringPass() {
		return enableByteArrayStringPass;
	}

	public double getByteArrayMinPrintableRatio() {
		return byteArrayMinPrintablePercent / 100.0;
	}

	public int getMaxCommentLength() {
		return maxCommentLength;
	}

	public double getMinPrintableRatio() {
		return minPrintablePercent / 100.0;
	}

	public double getMinAlphanumericRatio() {
		return minAlphanumericPercent / 100.0;
	}

	public boolean isRequireValidLength() {
		return requireValidLength;
	}

	public boolean isSkipCamelCase() {
		return skipCamelCase;
	}

	public boolean isSkipPascalCase() {
		return skipPascalCase;
	}

	public boolean isSkipSnakeCase() {
		return skipSnakeCase;
	}

	public boolean isSkipDictionaryWords() {
		return skipDictionaryWords;
	}

	public int getMinDecodedLength() {
		return minDecodedLength;
	}
}
