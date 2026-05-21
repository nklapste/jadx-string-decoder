package jadx.plugins.stringdecoder;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class B64DeobfuscateOptions extends BasePluginOptionsBuilder {

	private boolean enable;

	@Override
	public void registerOptions() {
		boolOption(JadxStringDecoderPlugin.PLUGIN_ID + ".enable")
				.description("Enable Base64 string detection and decoding")
				.defaultValue(true)
				.setter(v -> enable = v);
	}

	public boolean isEnable() {
		return enable;
	}
}
