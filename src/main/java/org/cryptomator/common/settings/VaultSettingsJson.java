package org.cryptomator.common.settings;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultSettingsJson {

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
	int port = VaultSettings.DEFAULT_PORT;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "winDriveLetter", access = JsonProperty.Access.WRITE_ONLY)
	String winDriveLetter;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "useCustomMountPath", access = JsonProperty.Access.WRITE_ONLY)
	@JsonAlias("usesIndividualMountPath")
	boolean useCustomMountPath;

	@Deprecated(since = "1.7.0")
	@JsonProperty(value = "customMountPath", access = JsonProperty.Access.WRITE_ONLY)
	@JsonAlias("individualMountPath")
	String customMountPath;

	// Explicit public no-args constructor
	public VaultSettingsJson() {
		// Fields will be initialized to their default values as above.
	}

	// Public constructor that initializes all key properties.
	public VaultSettingsJson(String id, String path, String displayName, boolean unlockAfterStartup, boolean revealAfterMount,
							 String mountPoint, boolean usesReadOnlyMode, String mountFlags, int maxCleartextFilenameLength,
							 WhenUnlocked actionAfterUnlock, boolean autoLockWhenIdle, int autoLockIdleSeconds,
							 String mountService, int port) {
		this.id = id;
		this.path = path;
		this.displayName = displayName;
		this.unlockAfterStartup = unlockAfterStartup;
		this.revealAfterMount = revealAfterMount;
		this.mountPoint = mountPoint;
		this.usesReadOnlyMode = usesReadOnlyMode;
		this.mountFlags = mountFlags;
		this.maxCleartextFilenameLength = maxCleartextFilenameLength;
		this.actionAfterUnlock = actionAfterUnlock;
		this.autoLockWhenIdle = autoLockWhenIdle;
		this.autoLockIdleSeconds = autoLockIdleSeconds;
		this.mountService = mountService;
		this.port = port;
	}

	// Public setters for each property

	public void setId(String id) {
		this.id = id;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setUnlockAfterStartup(boolean unlockAfterStartup) {
		this.unlockAfterStartup = unlockAfterStartup;
	}

	public void setRevealAfterMount(boolean revealAfterMount) {
		this.revealAfterMount = revealAfterMount;
	}

	public void setMountPoint(String mountPoint) {
		this.mountPoint = mountPoint;
	}

	public void setUsesReadOnlyMode(boolean usesReadOnlyMode) {
		this.usesReadOnlyMode = usesReadOnlyMode;
	}

	public void setMountFlags(String mountFlags) {
		this.mountFlags = mountFlags;
	}

	public void setMaxCleartextFilenameLength(int maxCleartextFilenameLength) {
		this.maxCleartextFilenameLength = maxCleartextFilenameLength;
	}

	public void setActionAfterUnlock(WhenUnlocked actionAfterUnlock) {
		this.actionAfterUnlock = actionAfterUnlock;
	}

	public void setAutoLockWhenIdle(boolean autoLockWhenIdle) {
		this.autoLockWhenIdle = autoLockWhenIdle;
	}

	public void setAutoLockIdleSeconds(int autoLockIdleSeconds) {
		this.autoLockIdleSeconds = autoLockIdleSeconds;
	}

	public void setMountService(String mountService) {
		this.mountService = mountService;
	}

	public void setPort(int port) {
		this.port = port;
	}
}