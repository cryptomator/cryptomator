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

class AdminPropertiesSetter {

	private static final Logger LOG = LoggerFactory.getLogger(AdminPropertiesSetter.class);

	static {
		final Path adminDir;
		if (SystemUtils.IS_OS_WINDOWS) {
			adminDir = Path.of(System.getenv().getOrDefault("ProgramData", "C:\\ProgramData"), "Cryptomator");
		} else if (SystemUtils.IS_OS_MAC) {
			adminDir = Path.of("/Library/Application Support/Cryptomator");
		} else { //LINUX
			adminDir = Path.of("/etc/cryptmator");
		}
		ADMIN_PROPERTIES_DIR = adminDir;
	}

	private static final Path ADMIN_PROPERTIES_DIR;
	private static final Path ADMIN_PROPERTIES_FILE = ADMIN_PROPERTIES_DIR.resolve("config.properties");

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
		} catch (IOException | IllegalArgumentException e) {
			LOG.warn("Failed to read administrative properties from {}. Returning empty properties.", adminPropertiesPath, e);
		}
		return adminProps;
	}

}
