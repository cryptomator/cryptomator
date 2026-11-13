import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Adds the font sequences for Chinese locales with UTF-8 default charset, which are missing in the JDK's Windows font configuration, to a runtime image.
 * <p>
 * Without them, native AWT components like the tray menu fall back to Arial and render Chinese text as squares, see https://github.com/cryptomator/cryptomator/issues/1474.
 * Run after {@code jlink} and before {@code jpackage} on the runtime image, e.g. {@code $JAVA_HOME/bin/java dist/win/AddChineseFontConfigSequences.java runtime}.
 * The JDK reads the resulting {@code lib/fontconfig.properties} in preference to the precompiled {@code lib/fontconfig.bfc}.
 */
public class AddChineseFontConfigSequences {

	// same component fonts as the existing GBK, x-windows-950 and x-MS950-HKSCS sequences
	private static final Map<String, String> CHINESE_SEQUENCES = Map.of( //
			"sequence.allfonts.UTF-8.zh", "alphabetic,chinese-ms936,dingbats,symbol,chinese-ms936-extb", //
			"sequence.allfonts.UTF-8.zh.TW", "alphabetic,chinese-ms950,dingbats,symbol,chinese-ms950-extb", //
			"sequence.allfonts.UTF-8.zh.HK", "alphabetic,chinese-ms950,chinese-hkscs,dingbats,symbol,chinese-ms950-extb" //
	);

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java AddChineseFontConfigSequences.java <path/to/runtime>");
			System.exit(2);
		}
		var libDir = Path.of(args[0], "lib");
		var fontConfig = new Properties();
		try (var in = Files.newInputStream(libDir.resolve("fontconfig.properties.src"))) {
			fontConfig.load(in);
		}
		for (var sequence : CHINESE_SEQUENCES.keySet()) {
			if (fontConfig.containsKey(sequence)) {
				System.err.println(sequence + " is already defined by the JDK, check if this workaround is still needed");
				System.exit(1);
			}
		}
		fontConfig.putAll(CHINESE_SEQUENCES);
		var target = libDir.resolve("fontconfig.properties");
		try (var out = Files.newOutputStream(target)) {
			fontConfig.store(out, "fontconfig.properties.src with added UTF-8.zh sequences, see dist/win/AddChineseFontConfigSequences.java");
		}
		System.out.println("Wrote " + target + " with UTF-8.zh font sequences");
	}
}
