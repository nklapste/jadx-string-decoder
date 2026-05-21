package jadx.plugins.example;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class B64DeobfuscateOptions extends BasePluginOptionsBuilder {

	private boolean enable;

	@Override
	public void registerOptions() {
		boolOption(JadxExamplePlugin.PLUGIN_ID + ".enable")
				.description("Enable Base64 string detection and decoding")
				.defaultValue(true)
				.setter(v -> enable = v);
	}

	public boolean isEnable() {
		return enable;
	}
}
