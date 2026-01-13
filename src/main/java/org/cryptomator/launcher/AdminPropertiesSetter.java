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

/**
 * Class to change JVM system properties according to an external properties file
 * <p>
 * The app starts with a set of predefined properties (defined in [APPDIR]/../Cryptomator.cfg).
 * That file is difficult to edit and edits do not persist over updates.
 * To allow overwriting these properties, the method {@link #adjustSystemProperties()} loads the properties file {@value PROP_FILENAME} from an OS-dependent location and add the contained properties to the {@link System} properties.
 * The predefined location are:
 *     <ul>
 *         <li>Linux - {@value LINUX_DIR }</li>
 *         <li>macOS - {@value MAC_DIR }</li>
 *         <li>Windows - {@value WIN_DIR }</li>
 *     </ul>
 * </p>
 *
 * @see System#getProperties()
 */
class AdminPropertiesSetter {

	private static final Logger LOG = LoggerFactory.getLogger(AdminPropertiesSetter.class);

	private static final String LINUX_DIR = "/etc/cryptmator";
	private static final String MAC_DIR = "/Library/Application Support/Cryptomator";
	private static final String WIN_DIR = "%PROGRAMDATA%\\Cryptomator";
	private static final String PROP_FILENAME = "config.properties";

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
	 * If the file exists and is a valid properties file, its properties will overwrite the existing system properties.
	 * <p>
	 * <em>WARNING:</em> This method modifies the global system properties and should be used with caution. Overwriting some properties has no effect, because they are read only once from the original config during JVM startup.
	 *
	 * @return The adjusted system properties.
	 */
	static Properties adjustSystemProperties() {
		var systemProps = System.getProperties();
		var adminProps = loadAdminProperties(ADMIN_PROPERTIES_FILE);

		for (var key : adminProps.stringPropertyNames()) {
			var value = adminProps.getProperty(key);
			LOG.info("Overwriting {} with value {} from admin properties.", key, value);
			systemProps.setProperty(key, value);
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
