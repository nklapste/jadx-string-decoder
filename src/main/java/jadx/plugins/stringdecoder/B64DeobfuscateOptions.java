package jadx.plugins.stringdecoder;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class B64DeobfuscateOptions extends BasePluginOptionsBuilder {

	private boolean enable;
	private int minInputLength;
	private int minDecodedLength;
	private int maxCommentLength;
	private int minPrintablePercent;
	private int minAlphanumericPercent;
	private boolean requirePadding;
	private boolean skipIdentifiers;

	@Override
	public void registerOptions() {
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".enable")
				.description("Enable Base64 string detection and decoding")
				.defaultValue(true)
				.setter(v -> enable = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minInputLength")
				.description("Minimum length of an encoded string to be considered for decoding")
				.defaultValue(8)
				.setter(v -> minInputLength = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".maxCommentLength")
				.description("Maximum decoded string length in comment (0 for unlimited)")
				.defaultValue(100)
				.setter(v -> maxCommentLength = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minPrintablePercent")
				.description("Minimum percentage of printable ASCII chars in decoded string (0-100); higher values reduce false positives")
				.defaultValue(90)
				.setter(v -> minPrintablePercent = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minAlphanumericPercent")
				.description("Minimum percentage of alphanumeric chars in decoded string (0 = disabled); helps reject symbol-heavy garbage decodes")
				.defaultValue(35)
				.setter(v -> minAlphanumericPercent = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".requirePadding")
				.description("Only flag strings that end with '=' padding; reduces false positives from identifiers and short words")
				.defaultValue(false)
				.setter(v -> requirePadding = v);
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".skipIdentifiers")
				.description("Skip strings that look like Java identifiers (letters/digits only, starts with a letter); avoids flagging symbol names")
				.defaultValue(false)
				.setter(v -> skipIdentifiers = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".minDecodedLength")
				.description("Minimum decoded string length to add a comment (0 = disabled); rejects very short decoded outputs")
				.defaultValue(0)
				.setter(v -> minDecodedLength = v);
	}

	public boolean isEnable() {
		return enable;
	}

	public int getMinInputLength() {
		return minInputLength;
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

	public boolean isRequirePadding() {
		return requirePadding;
	}

	public boolean isSkipIdentifiers() {
		return skipIdentifiers;
	}

	public int getMinDecodedLength() {
		return minDecodedLength;
	}
}
