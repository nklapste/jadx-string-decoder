package jadx.plugins.stringdecoder;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.options.OptionDescription;

public class JadxStringDecoderPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "jadx-string-decoder";

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
		JadxGuiContext guiCtx = context.getGuiContext();
		if (guiCtx != null) {
			setupCustomSettingsGroup(guiCtx);
		}
		if (options.isEnable()) {
			context.addPass(new B64DeobfuscatePass(options));
			context.addPass(new B64FieldInitPass(options));
			if (options.isEnableByteArrayStringPass()) {
				context.addPass(new ByteArrayStringPass(options));
			}
		}
	}

	private void setupCustomSettingsGroup(JadxGuiContext guiCtx) {
		guiCtx.settings().setCustomSettingsGroup(new ISettingsGroup() {
			private JPanel panel;

			@Override
			public String getTitle() {
				return "String Decoder";
			}

			@Override
			public JComponent buildComponent() {
				if (panel == null) {
					List<OptionDescription> allOpts = options.getOptionsDescriptions();
					List<OptionDescription> b64Opts = allOpts.stream()
							.filter(o -> !o.name().contains("ByteArray"))
							.collect(Collectors.toList());
					List<OptionDescription> byteArrayOpts = allOpts.stream()
							.filter(o -> o.name().contains("ByteArray"))
							.collect(Collectors.toList());

					JComponent b64Panel = guiCtx.settings()
							.buildSettingsGroupForOptions("B64 String Decoder", b64Opts)
							.buildComponent();
					JComponent byteArrayPanel = guiCtx.settings()
							.buildSettingsGroupForOptions("Byte Array String Decoder", byteArrayOpts)
							.buildComponent();

					panel = new JPanel();
					panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
					panel.add(b64Panel);
					panel.add(byteArrayPanel);
				}
				return panel;
			}
		});
	}
}
