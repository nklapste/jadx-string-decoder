package jadx.plugins.stringdecoder;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

abstract class PluginTestBase {

	protected String decompileSmali(String fileName) throws Exception {
		return decompileSmali(fileName, Map.of());
	}

	protected String decompileSmali(String fileName, Map<String, String> pluginOptions) throws Exception {
		JadxArgs args = new JadxArgs();
		args.isShowInconsistentCode();
		args.getInputFiles().add(getSampleFile(fileName));
		args.setPluginOptions(pluginOptions);
		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			JavaClass cls = jadx.getClasses().get(0);
			return cls.getCode();
		}
	}

	protected static String opt(String key) {
		return JadxStringDecoderPlugin.PLUGIN_ID + "." + key;
	}

	private File getSampleFile(String fileName) throws URISyntaxException {
		URL file = getClass().getClassLoader().getResource("samples/" + fileName);
		assertThat(file).isNotNull();
		return new File(file.toURI());
	}
}
