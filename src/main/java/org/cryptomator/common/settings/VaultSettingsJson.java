package org.cryptomator.common.settings;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class VaultSettingsJson {

	@JsonProperty(value = "id", required = true)
	String id;

	@JsonProperty(value = "path")
	String path;

	@JsonProperty("displayName")
	String displayName;

	@JsonProperty("unlockAfterStartup")
	boolean unlockAfterStartup = VaultSettings.DEFAULT_UNLOCK_AFTER_STARTUP;

	@JsonProperty("revealAfterMount")
	boolean revealAfterMount = VaultSettings.DEFAULT_REVEAL_AFTER_MOUNT;

	@JsonProperty("mountPoint")
	String mountPoint;

	@JsonProperty("usesReadOnlyMode")
	boolean usesReadOnlyMode = VaultSettings.DEFAULT_USES_READONLY_MODE;

	@JsonProperty("mountFlags")
	String mountFlags = VaultSettings.DEFAULT_MOUNT_FLAGS;

	@JsonProperty("maxCleartextFilenameLength")
	int maxCleartextFilenameLength = VaultSettings.DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH;

	@JsonProperty("actionAfterUnlock")
	WhenUnlocked actionAfterUnlock = VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK;

	@JsonProperty("autoLockWhenIdle")
	boolean autoLockWhenIdle = VaultSettings.DEFAULT_AUTOLOCK_WHEN_IDLE;

	@JsonProperty("autoLockIdleSeconds")
	int autoLockIdleSeconds = VaultSettings.DEFAULT_AUTOLOCK_IDLE_SECONDS;

	@JsonProperty("mountService")
	String mountService;

	@JsonProperty("port")
	int port = Settings.DEFAULT_PORT;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "winDriveLetter", access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY means value is "written" into the java object during deserialization. Upvote this: https://github.com/FasterXML/jackson-annotations/issues/233
	String winDriveLetter;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "useCustomMountPath", access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY means value is "written" into the java object during deserialization. Upvote this: https://github.com/FasterXML/jackson-annotations/issues/233
	@JsonAlias("usesIndividualMountPath")
	boolean useCustomMountPath;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "customMountPath", access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY means value is "written" into the java object during deserialization. Upvote this: https://github.com/FasterXML/jackson-annotations/issues/233
	@JsonAlias("individualMountPath")
	String customMountPath;

}
