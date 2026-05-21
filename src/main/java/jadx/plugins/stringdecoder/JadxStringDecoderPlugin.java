package jadx.plugins.stringdecoder;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

public class JadxStringDecoderPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "b64-deobfuscate";

	private final B64DeobfuscateOptions options = new B64DeobfuscateOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("String Decoder")
				.description("Detect likely Base64-encoded string constants and add decoded value as a comment")
				.homepage("https://github.com/nklapste/jadx-string-decoder")
				.requiredJadxVersion("1.5.1, r2333")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		if (options.isEnable()) {
			context.addPass(new B64DeobfuscatePass());
		}
	}
}
