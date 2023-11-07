package org.cryptomator.common.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class SettingsJson {

	@JsonProperty("directories")
	List<VaultSettingsJson> directories = List.of();

	@JsonProperty("writtenByVersion")
	String writtenByVersion;

	@JsonProperty("askedForUpdateCheck")
	boolean askedForUpdateCheck = Settings.DEFAULT_ASKED_FOR_UPDATE_CHECK;

	@JsonProperty("autoCloseVaults")
	boolean autoCloseVaults = Settings.DEFAULT_AUTO_CLOSE_VAULTS;

	@JsonProperty("checkForUpdatesEnabled")
	boolean checkForUpdatesEnabled = Settings.DEFAULT_CHECK_FOR_UPDATES;

	@JsonProperty("debugMode")
	boolean debugMode = Settings.DEFAULT_DEBUG_MODE;

	@JsonProperty("theme")
	UiTheme theme = Settings.DEFAULT_THEME;

	@JsonProperty("keychainProvider")
	String keychainProvider = Settings.DEFAULT_KEYCHAIN_PROVIDER;

	@JsonProperty("language")
	String language;

	@JsonProperty("licenseKey")
	String licenseKey;

	@JsonProperty("mountService")
	String mountService;

	@JsonProperty("numTrayNotifications")
	int numTrayNotifications = Settings.DEFAULT_NUM_TRAY_NOTIFICATIONS;

	@Deprecated(since = "1.10.2")
	@JsonProperty(value = "port", access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY means value is "written" into the java object during deserialization. Upvote this: https://github.com/FasterXML/jackson-annotations/issues/233
	int port;

	@JsonProperty("showMinimizeButton")
	boolean showMinimizeButton = Settings.DEFAULT_SHOW_MINIMIZE_BUTTON;

	@JsonProperty("showTrayIcon")
	boolean showTrayIcon;

	@JsonProperty("startHidden")
	boolean startHidden = Settings.DEFAULT_START_HIDDEN;

	@JsonProperty("uiOrientation")
	String uiOrientation = Settings.DEFAULT_USER_INTERFACE_ORIENTATION;

	@JsonProperty("useKeychain")
	boolean useKeychain = Settings.DEFAULT_USE_KEYCHAIN;

	@JsonProperty("windowHeight")
	int windowHeight;

	@JsonProperty("windowWidth")
	int windowWidth;

	@JsonProperty("windowXPosition")
	int windowXPosition;

	@JsonProperty("windowYPosition")
	int windowYPosition;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "preferredVolumeImpl", access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY means value is "written" into the java object during deserialization. Upvote this: https://github.com/FasterXML/jackson-annotations/issues/233
	String preferredVolumeImpl;

	@JsonProperty("lastUpdateCheck")
	String lastUpdateCheck = Settings.DEFAULT_LAST_UPDATE_CHECK;

}
