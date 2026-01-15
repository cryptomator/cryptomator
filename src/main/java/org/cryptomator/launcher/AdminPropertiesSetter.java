package org.cryptomator.launcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Class to overwrite system properties with an external properties file
 * <p>
 * To overwrite system properties, the method {@link #adjustSystemProperties()} loads the JSON file {@value CONFIG_NAME} from an OS-dependent location and adds all whitelisted properties to the {@link System} properties.
 * The predefined locations are:
 * <ul>
 *     <li>Linux - {@value LINUX_DIR }</li>
 *    <li>macOS - {@value MAC_DIR }</li>
 *    <li>Windows - {@value WIN_DIR }</li>
 * </ul>
 * </p>
 * <p>
 * The overridable properties are:
 * <ul>
 *     <li>cryptomator.logDir</li>
 *     <li>cryptomator.pluginDir</li>
 *     <li>cryptomator.p12Path</li>
 *     <li>cryptomator.mountPointsDir</li>
 *     <li>cryptomator.disableUpdateCheck</li>
 * </ul>
 *
 * @see System#getProperties()
 */
class AdminPropertiesSetter {

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	private static final String LINUX_DIR = "/etc/cryptomator";
	private static final String MAC_DIR = "/Library/Application Support/Cryptomator";
	private static final String WIN_DIR = "%PROGRAMDATA%\\Cryptomator";
	private static final String CONFIG_NAME = "config.json";
	private static final Set<String> ALLOWED_OVERRIDES = Set.of( //
			"cryptomator.logDir", //
			"cryptomator.pluginDir", //
			"cryptomator.p12Path", //
			"cryptomator.mountPointsDir", //
			"cryptomator.disableUpdateCheck");


	static {
		final Path adminDir;
		if (SystemUtils.IS_OS_WINDOWS) {
			adminDir = Path.of(System.getenv().getOrDefault("ProgramData", "C:\\ProgramData"), "Cryptomator");
		} else if (SystemUtils.IS_OS_MAC) {
			adminDir = Path.of(MAC_DIR);
		} else { //LINUX
			adminDir = Path.of(LINUX_DIR);
		}
		ADMIN_PROPERTIES_FILE = adminDir.resolve(CONFIG_NAME);
	}

	private static final Path ADMIN_PROPERTIES_FILE;

	/**
	 * Adjusts the system properties by loading administrative properties from a predefined file location.
	 * <p>
	 * If the file exists and is a valid JSON file, its content will overwrite existing system properties.
	 * Only some properties can be overridden, see {@link AdminPropertiesSetter}
	 *
	 * @return The adjusted system properties.
	 */
	static Properties adjustSystemProperties() {
		var systemProps = System.getProperties();
		var adminProps = loadAdminProperties(ADMIN_PROPERTIES_FILE);

		adminProps.forEach((key, value) -> {
			if (ALLOWED_OVERRIDES.contains(key) && value instanceof String v) {
				log("Overwriting {} with value {} from admin properties.", List.of(key, v));
				systemProps.setProperty(key, v);
			} else {
				var reason = value instanceof String ? "Unsupported" : "Not a string";
				log("Property {} in admin config ignored: {}.", List.of(key, reason));
			}
		});
		return systemProps;
	}

	private static Map<String, Object> loadAdminProperties(Path adminPropertiesPath) {
		try (var in = Files.newInputStream(adminPropertiesPath, StandardOpenOption.READ)) {
			var map = JSON.readValue(in, new TypeReference<Map<String, Object>>() {});
			if (map == null) {
				throw new NullPointerException("JSON parsing returned null");
			}
			return map;
		} catch (NoSuchFileException _) {
			//NO-OP
			log("No admin properties found at {}.", List.of(adminPropertiesPath));
		} catch (IOException | RuntimeException e) {
			log("Failed to read administrative properties from {}. Returning empty properties.", List.of(adminPropertiesPath, e));
		}
		return Map.of();
	}

	static void log(String message, List<Object> messageInput) {
		BufferedLog.log(AdminPropertiesSetter.class.getName(), message, messageInput);
	}
}
