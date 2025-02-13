package org.cryptomator.common;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Environment {

	private static final Logger LOG = LoggerFactory.getLogger(Environment.class);
	private static final int DEFAULT_MIN_PW_LENGTH = 8;
	private static final String SETTINGS_PATH_PROP_NAME = "cryptomator.settingsPath";
	private static final String IPC_SOCKET_PATH_PROP_NAME = "cryptomator.ipcSocketPath";
	private static final String KEYCHAIN_PATHS_PROP_NAME = "cryptomator.integrationsWin.keychainPaths";
	private static final String P12_PATH_PROP_NAME = "cryptomator.p12Path";
	private static final String LOG_DIR_PROP_NAME = "cryptomator.logDir";
	private static final String LOOPBACK_ALIAS_PROP_NAME = "cryptomator.loopbackAlias";
	private static final String MOUNTPOINT_DIR_PROP_NAME = "cryptomator.mountPointsDir";
	private static final String MIN_PW_LENGTH_PROP_NAME = "cryptomator.minPwLength";
	private static final String APP_VERSION_PROP_NAME = "cryptomator.appVersion";
	private static final String BUILD_NUMBER_PROP_NAME = "cryptomator.buildNumber";
	private static final String PLUGIN_DIR_PROP_NAME = "cryptomator.pluginDir";
	private static final String TRAY_ICON_PROP_NAME = "cryptomator.showTrayIcon";
	private static final String DISABLE_UPDATE_CHECK_PROP_NAME = "cryptomator.disableUpdateCheck";

	private Environment() {}

	public void log() {
		LOG.info("user.home: {}", System.getProperty("user.home"));
		LOG.info("java.library.path: {}", System.getProperty("java.library.path"));
		LOG.info("user.language: {}", System.getProperty("user.language"));
		LOG.info("user.region: {}", System.getProperty("user.region"));
		LOG.info("logback.configurationFile: {}", System.getProperty("logback.configurationFile"));
		logCryptomatorSystemProperty(SETTINGS_PATH_PROP_NAME);
		logCryptomatorSystemProperty(IPC_SOCKET_PATH_PROP_NAME);
		logCryptomatorSystemProperty(KEYCHAIN_PATHS_PROP_NAME);
		logCryptomatorSystemProperty(P12_PATH_PROP_NAME);
		logCryptomatorSystemProperty(LOG_DIR_PROP_NAME);
		logCryptomatorSystemProperty(LOOPBACK_ALIAS_PROP_NAME);
		logCryptomatorSystemProperty(MOUNTPOINT_DIR_PROP_NAME);
		logCryptomatorSystemProperty(MIN_PW_LENGTH_PROP_NAME);
		logCryptomatorSystemProperty(APP_VERSION_PROP_NAME);
		logCryptomatorSystemProperty(BUILD_NUMBER_PROP_NAME);
		logCryptomatorSystemProperty(PLUGIN_DIR_PROP_NAME);
		logCryptomatorSystemProperty(TRAY_ICON_PROP_NAME);
		logCryptomatorSystemProperty(DISABLE_UPDATE_CHECK_PROP_NAME);
	}

	public static Environment getInstance() {
		final class Holder {

			private static final Environment INSTANCE = new Environment();
		}
		return Holder.INSTANCE;
	}

	private void logCryptomatorSystemProperty(String propertyName) {
		LOG.info("{}: {}", propertyName, System.getProperty(propertyName));
	}

	public boolean useCustomLogbackConfig() {
		return getPath("logback.configurationFile").map(Files::exists).orElse(false);
	}

	public Stream<Path> getSettingsPath() {
		return getPaths(SETTINGS_PATH_PROP_NAME);
	}

	public Stream<Path> getIpcSocketPath() {
		return getPaths(IPC_SOCKET_PATH_PROP_NAME);
	}

	public Stream<Path> getKeychainPath() {
		return getPaths(KEYCHAIN_PATHS_PROP_NAME);
	}

	public Stream<Path> getP12Path() {
		return getPaths(P12_PATH_PROP_NAME);
	}

	public Optional<Path> getLogDir() {
		return getPath(LOG_DIR_PROP_NAME);
	}

	public Optional<String> getLoopbackAlias() {
		return Optional.ofNullable(System.getProperty(LOOPBACK_ALIAS_PROP_NAME));
	}

	public Optional<Path> getMountPointsDir() {
		return getPath(MOUNTPOINT_DIR_PROP_NAME);
	}

	public int getMinPwLength() {
		return Integer.getInteger(MIN_PW_LENGTH_PROP_NAME, DEFAULT_MIN_PW_LENGTH);
	}

	/**
	 * Returns the app version defined in the {@value APP_VERSION_PROP_NAME} property or returns "SNAPSHOT".
	 *
	 * @return App version or "SNAPSHOT", if undefined
	 */
	public String getAppVersion() {
		return System.getProperty(APP_VERSION_PROP_NAME, "1.14.2");
	}

	public Optional<String> getBuildNumber() {
		return Optional.ofNullable(System.getProperty(BUILD_NUMBER_PROP_NAME));
	}

	public Optional<Path> getPluginDir() {
		return getPath(PLUGIN_DIR_PROP_NAME);
	}

	public boolean showTrayIcon() {
		return Boolean.getBoolean(TRAY_ICON_PROP_NAME);
	}

	public boolean disableUpdateCheck() {
		return Boolean.getBoolean(DISABLE_UPDATE_CHECK_PROP_NAME);
	}

	private Optional<Path> getPath(String propertyName) {
		String value = System.getProperty(propertyName);
		return Optional.ofNullable(value).map(Paths::get);
	}

	@VisibleForTesting
	Stream<Path> getPaths(String propertyName) {
		Stream<String> rawSettingsPaths = getRawList(propertyName, System.getProperty("path.separator").charAt(0));
		return rawSettingsPaths.filter(Predicate.not(Strings::isNullOrEmpty)).map(Path::of);
	}

	private Stream<String> getRawList(String propertyName, char separator) {
		String value = System.getProperty(propertyName);
		if (value == null) {
			return Stream.empty();
		} else {
			Iterable<String> iter = Splitter.on(separator).split(value);
			Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(iter.iterator(), Spliterator.ORDERED | Spliterator.IMMUTABLE);
			return StreamSupport.stream(spliterator, false);
		}
	}
}
