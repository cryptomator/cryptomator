package org.cryptomator.launcher;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.Set;

/**
 * Factory to generate admin properties.
 *
 * <p>
 * Admin properties are {@link Properties} using system properties as defaults, but allow overwriting a specific set of properties with an external config file.
 * Those properties are created by calling {@link #create()}. The method first reads system property {@value #ADMIN_PROP_FILE_KEY}. If it contains a path to a valid properties file, all overridable properties from the file are loaded into the returned admin properties.
 * On Windows, the file is ignored unless only SYSTEM, Administrators or TrustedInstaller can modify it.
 * <p>
 * The overridable properties are:
 * <ul>
 *     <li>cryptomator.logDir</li>
 *     <li>cryptomator.pluginDir</li>
 *     <li>cryptomator.p12Path</li>
 *     <li>cryptomator.mountPointsDir</li>
 *     <li>cryptomator.disableUpdateCheck</li>
 *     <li>cryptomator.hub.allowedHosts</li>
 *     <li>cryptomator.hub.enableTrustOnFirstUse</li>
 * </ul>
 *
 * @see Properties
 * @see System#getProperties()
 */
class AdminPropertiesFactory {

	private static final Logger LOG = EventualLogger.INSTANCE;
	private static final long MAX_CONFIG_SIZE_BYTES = 8192;
	private static final String ADMIN_PROP_FILE_KEY = "cryptomator.adminConfigPath";
	private static final Set<String> ALLOWED_OVERRIDES = Set.of( //
			"cryptomator.logDir", //
			"cryptomator.pluginDir", //
			"cryptomator.p12Path", //
			"cryptomator.mountPointsDir", //
			"cryptomator.disableUpdateCheck", //
 			"cryptomator.hub.allowedHosts", //
			"cryptomator.hub.enableTrustOnFirstUse");


	/**
	 * Creates new {@link Properties} containing overridable properties from the admin config.
	 * <p>
	 * The returned properties object uses as default the {@link System} properties.
	 * For a list of overridable properties, see {@link AdminPropertiesFactory}
	 *
	 * @return {@link Properties} containing overridable properties from the admin config and defaulting to system properties.
	 */
	static Properties create() {
		var systemProps = System.getProperties();
		var adminProps = new Properties(systemProps);

		final String adminCfgPath = System.getProperty(ADMIN_PROP_FILE_KEY);
		if (adminCfgPath == null) {
			LOG.debug("Admin config property is not defined. Skipping.");
			return adminProps;
		}
		var adminCfg = Path.of(adminCfgPath);
		if (SystemUtils.IS_OS_WINDOWS && !isProtectedOnWindows(adminCfg)) {
			return adminProps;
		}
		var propsFromFile = loadPropertiesFromFile(adminCfg);

		for (var key : propsFromFile.stringPropertyNames()) {
			if (ALLOWED_OVERRIDES.contains(key)) {
				var value = propsFromFile.getProperty(key);
				LOG.info("Overwriting {} with value {} from admin config.", key, value);
				adminProps.setProperty(key, value);
			} else {
				LOG.debug("Property {} in admin config is not supported for override.", key);
			}
		}
		return adminProps;
	}

	// a standard user able to modify the admin config could e.g. make other users load plugins from an arbitrary directory
	private static boolean isProtectedOnWindows(Path adminCfg) {
		try {
			if (WindowsAdminConfigVerifier.isProtected(adminCfg)) {
				return true;
			}
			LOG.warn("Ignoring admin config {}, because it or one of its directories can be modified by non-administrative principals. Restrict owner and write access to SYSTEM and Administrators, or delete the directory containing it and reinstall Cryptomator.", adminCfg);
		} catch (NoSuchFileException _) {
			// do not proceed to load the file, it might have been created meanwhile by someone else
			LOG.debug("No admin properties found at {}.", adminCfg);
		} catch (IOException e) {
			LOG.warn("Ignoring admin config {}, because its permissions could not be verified.", adminCfg, e);
		}
		return false;
	}

	//visible for testing
	static Properties loadPropertiesFromFile(Path adminPropertiesPath) {
		var adminProps = new Properties();
		try (FileChannel ch = FileChannel.open(adminPropertiesPath, StandardOpenOption.READ); //
			 Reader reader = Channels.newReader(ch, StandardCharsets.UTF_8)) {
			if (ch.size() > MAX_CONFIG_SIZE_BYTES) {
				throw new IOException("Config file %s exceeds maximum size of %d".formatted(adminPropertiesPath, MAX_CONFIG_SIZE_BYTES));
			}
			adminProps.load(reader);
		} catch (NoSuchFileException _) {
			//NO-OP
			LOG.debug("No admin properties found at {}.", adminPropertiesPath);
		} catch (IOException | IllegalArgumentException e) {
			LOG.warn("Failed to read administrative properties from {}. Returning empty properties.", adminPropertiesPath, e);
		}
		return adminProps;
	}

}
