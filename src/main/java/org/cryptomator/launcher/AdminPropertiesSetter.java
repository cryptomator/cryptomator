package org.cryptomator.launcher;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.Set;

/**
 * Class to overwrite system properties with an external properties file
 * <p>
 * To overwrite system properties, the method {@link #adjustSystemProperties()} loads the properties file {@value PROP_FILENAME} from an OS-dependent location and add all supported properties to the {@link System} properties.
 * The predefined location are:
 *     <ul>
 *         <li>Linux - {@value LINUX_DIR }</li>
 *         <li>macOS - {@value MAC_DIR }</li>
 *         <li>Windows - {@value WIN_DIR }</li>
 *     </ul>
 * </p>
 * <p>
 * Supported properties for override are:
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

	private static final Logger LOG = LoggerFactory.getLogger(AdminPropertiesSetter.class);

	private static final String LINUX_DIR = "/etc/cryptmator";
	private static final String MAC_DIR = "/Library/Application Support/Cryptomator";
	private static final String WIN_DIR = "%PROGRAMDATA%\\Cryptomator";
	private static final String PROP_FILENAME = "config.properties";
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
		ADMIN_PROPERTIES_FILE = adminDir.resolve(PROP_FILENAME);
	}

	private static final Path ADMIN_PROPERTIES_FILE;

	/**
	 * Adjusts the system properties by loading administrative properties from a predefined file location.
	 * <p>
	 * If the file exists and is a valid properties file, its content will overwrite existing system properties.
	 * Only some properties are supported, see {@link AdminPropertiesSetter}
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
		try {
			adminProps.load(Files.newInputStream(adminPropertiesPath, StandardOpenOption.READ));
		} catch (NoSuchFileException _) {
			//NO-OP
			LOG.debug("No admin properties found at  {}.", adminPropertiesPath);
		} catch (IOException | IllegalArgumentException e) {
			LOG.warn("Failed to read administrative properties from {}. Returning empty properties.", adminPropertiesPath, e);
		}
		return adminProps;
	}

}
