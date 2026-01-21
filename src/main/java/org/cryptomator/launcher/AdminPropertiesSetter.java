package org.cryptomator.launcher;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * Class to overwrite system properties with an external properties file
 * <p>
 * To overwrite system properties, the method {@link #adjustSystemProperties()} loads the properties file {@value CONFIG_NAME} from an OS-dependent location and adds all supported properties to the {@link System} properties.
 * The predefined location is for
 *     <ul>
 *         <li>Linux: {@value LINUX_DIR }</li>
 *         <li>macOS: {@value MAC_DIR }</li>
 *         <li>Windows: {@value WIN_DIR }</li>
 *     </ul>
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

	private static final Logger LOG = EventualLogger.getInstance();

	private static final String LINUX_DIR = "/etc/cryptomator";
	private static final String MAC_DIR = "/Library/Application Support/Cryptomator";
	private static final String WIN_DIR = "C:\\ProgramData\\Cryptomator";
	private static final String CONFIG_NAME = "config.properties";
	private static final Set<String> ALLOWED_OVERRIDES = Set.of( //
			"cryptomator.logDir", //
			"cryptomator.pluginDir", //
			"cryptomator.p12Path", //
			"cryptomator.mountPointsDir", //
			"cryptomator.disableUpdateCheck");


	static {
		final Path adminDir;
		if (SystemUtils.IS_OS_WINDOWS) {
			adminDir = Path.of(WIN_DIR);
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
	 * If the file exists and is a valid properties file, its content will overwrite existing system properties.
	 * Only some properties can be overridden, see {@link AdminPropertiesSetter}
	 *
	 * @return The adjusted system properties.
	 */
	static Properties adjustSystemProperties() {
		var systemProps = System.getProperties();
		var adminProps = loadAdminProperties(ADMIN_PROPERTIES_FILE);

		for (var key : adminProps.stringPropertyNames()) {
			if (ALLOWED_OVERRIDES.contains(key)) {
				var value = adminProps.getProperty(key);
				LOG.info("Overwriting {} with value {} from admin properties.", key, value);
				systemProps.setProperty(key, value);
			} else {
				LOG.debug("Property {} in admin properties is not supported for override.", key);
			}
		}
		return systemProps;
	}

	private static Properties loadAdminProperties(Path adminPropertiesPath) {
		var adminProps = new Properties();
		try (var reader = Files.newBufferedReader(adminPropertiesPath, StandardCharsets.UTF_8)) {
			adminProps.load(reader);
		} catch (NoSuchFileException _) {
			//NO-OP
			LOG.debug("No admin properties found at  {}.", adminPropertiesPath);
		} catch (IOException | IllegalArgumentException e) {
			LOG.warn("Failed to read administrative properties from {}. Returning empty properties.", adminPropertiesPath, e);
		}
		return adminProps;
	}

}
