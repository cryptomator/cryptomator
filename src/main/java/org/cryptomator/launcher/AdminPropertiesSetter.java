package org.cryptomator.launcher;

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
 * To overwrite system properties, the method {@link #adjustSystemProperties()} reads the properties file defined in the property {@value #ADMIN_PROP_FILE_KEY} and writes all supported properties to the {@link System} properties.
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

	private static final Logger LOG = EventualLogger.INSTANCE;
	private static final long MAX_CONFIG_SIZE_BYTES = 8192;
	private static final String ADMIN_PROP_FILE_KEY = "cryptomator.adminConfig";
	private static final Set<String> ALLOWED_OVERRIDES = Set.of( //
			"cryptomator.logDir", //
			"cryptomator.pluginDir", //
			"cryptomator.p12Path", //
			"cryptomator.mountPointsDir", //
			"cryptomator.disableUpdateCheck");


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

		final String systemPropertyDefinedAdminFile = System.getProperty(ADMIN_PROP_FILE_KEY);
		if (systemPropertyDefinedAdminFile == null) {
			LOG.debug("Path to admin properties file is not defined.");
			return systemProps;
		}
		var adminProps = loadAdminProperties(Path.of(systemPropertyDefinedAdminFile));

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

	//visible for testing
	static Properties loadAdminProperties(Path adminPropertiesPath) {
		var adminProps = new Properties();
		try {
			if (Files.size(adminPropertiesPath) > MAX_CONFIG_SIZE_BYTES) {
				throw new IOException("Config file %s exceeds maximum size of %d".formatted(adminPropertiesPath, MAX_CONFIG_SIZE_BYTES));
			}
			try (var reader = Files.newBufferedReader(adminPropertiesPath, StandardCharsets.UTF_8)) {
				adminProps.load(reader);
			}
		} catch (NoSuchFileException _) {
			//NO-OP
			LOG.debug("No admin properties found at {}.", adminPropertiesPath);
		} catch (IOException | IllegalArgumentException e) {
			LOG.warn("Failed to read administrative properties from {}. Returning empty properties.", adminPropertiesPath, e);
		}
		return adminProps;
	}

}
