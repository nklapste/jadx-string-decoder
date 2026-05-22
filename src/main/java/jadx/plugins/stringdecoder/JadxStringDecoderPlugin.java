package jadx.plugins.stringdecoder;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
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

	private static JComponent buildSection(JadxGuiContext guiCtx, String title,
			List<OptionDescription> opts, String note) {
		JComponent panel = guiCtx.settings().buildSettingsGroupForOptions(title, opts).buildComponent();
		// panel is a BorderLayout JPanel with a TitledBorder; the option grid sits at PAGE_START.
		// Pull the grid out, wrap it with the note label, and re-insert so the note is inside the border.
		if (panel.getLayout() instanceof BorderLayout) {
			BorderLayout bl = (BorderLayout) panel.getLayout();
			Component grid = bl.getLayoutComponent(BorderLayout.PAGE_START);
			if (grid != null) {
				panel.remove(grid);
				JLabel noteLabel = new JLabel(note);
				noteLabel.setEnabled(false);
				noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				if (grid instanceof JComponent) {
					((JComponent) grid).setAlignmentX(Component.LEFT_ALIGNMENT);
				}
				JPanel inner = new JPanel();
				inner.setLayout(new BoxLayout(inner, BoxLayout.PAGE_AXIS));
				inner.add(noteLabel);
				inner.add(grid);
				panel.add(inner, BorderLayout.PAGE_START);
			}
		}
		return panel;
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
					Set<String> generalSuffixes = Set.of("minInputLength", "maxCommentLength", "minDecodedLength");
					List<OptionDescription> allOpts = options.getOptionsDescriptions();
					List<OptionDescription> generalOpts = allOpts.stream()
							.filter(o -> generalSuffixes.stream().anyMatch(s -> o.name().endsWith("." + s)))
							.collect(Collectors.toList());
					List<OptionDescription> byteArrayOpts = allOpts.stream()
							.filter(o -> o.name().toLowerCase().contains("bytearray"))
							.collect(Collectors.toList());
					List<OptionDescription> b64Opts = allOpts.stream()
							.filter(o -> !generalOpts.contains(o) && !byteArrayOpts.contains(o))
							.collect(Collectors.toList());

					panel = new JPanel();
					panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
					panel.add(JadxStringDecoderPlugin.buildSection(guiCtx, "General", generalOpts,
							"* Options shared across all detection passes."));
					panel.add(JadxStringDecoderPlugin.buildSection(guiCtx, "B64 String Decoder", b64Opts,
							"* Adds // b64: <DECODED_VALUE> comments to Base64-encoded string/field initializers."));
					panel.add(JadxStringDecoderPlugin.buildSection(guiCtx, "Byte Array String Decoder", byteArrayOpts,
							"* Adds // bytes: <DECODED_VALUE> comments to byte[] fields that decode to printable strings."));
				}
				return panel;
			}
		});
	}
}
