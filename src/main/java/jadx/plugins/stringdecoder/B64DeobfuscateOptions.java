package jadx.plugins.stringdecoder;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class B64DeobfuscateOptions extends BasePluginOptionsBuilder {

	private boolean enable;
	private int maxCommentLength;

	@Override
	public void registerOptions() {
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".enable")
				.description("Enable Base64 string detection and decoding")
				.defaultValue(true)
				.setter(v -> enable = v);
		intOption(JadxStringDecoderPlugin.PLUGIN_ID + ".maxCommentLength")
				.description("Maximum decoded string length in comment (0 for unlimited)")
				.defaultValue(100)
				.setter(v -> maxCommentLength = v);
	}

	public boolean isEnable() {
		return enable;
	}

	public int getMaxCommentLength() {
		return maxCommentLength;
	}
}
