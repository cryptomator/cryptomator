/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.SystemUtils;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;

/**
 * The settings specific to a single vault.
 */
public class VaultSettings {

	static final boolean DEFAULT_UNLOCK_AFTER_STARTUP = false;
	static final boolean DEFAULT_REVEAL_AFTER_MOUNT = true;
	static final boolean DEFAULT_USES_READONLY_MODE = false;
	static final String DEFAULT_MOUNT_FLAGS = ""; // TODO: remove empty default mount flags and let this property be null if not used
	static final int DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH = -1;
	static final WhenUnlocked DEFAULT_ACTION_AFTER_UNLOCK = WhenUnlocked.ASK;
	static final boolean DEFAULT_AUTOLOCK_WHEN_IDLE = false;
	static final int DEFAULT_AUTOLOCK_IDLE_SECONDS = 30 * 60;

	private static final Random RNG = new Random();

	private final String id;
	private final ObjectProperty<Path> path;
	private final StringProperty displayName;
	private final BooleanProperty unlockAfterStartup;
	private final BooleanProperty revealAfterMount;
	private final BooleanProperty usesReadOnlyMode;
	private final StringProperty mountFlags;
	private final IntegerProperty maxCleartextFilenameLength;
	private final ObjectProperty<WhenUnlocked> actionAfterUnlock;
	private final BooleanProperty autoLockWhenIdle;
	private final IntegerProperty autoLockIdleSeconds;
	private final ObjectProperty<Path> mountPoint;
	private final StringExpression mountName;

	VaultSettings(VaultSettingsJson json) {
		this.id = json.id;
		this.path = new SimpleObjectProperty<>(this, "path", json.path == null ? null : Paths.get(json.path));
		this.displayName = new SimpleStringProperty(this, "displayName", json.displayName);
		this.unlockAfterStartup = new SimpleBooleanProperty(this, "unlockAfterStartup", json.unlockAfterStartup);
		this.revealAfterMount = new SimpleBooleanProperty(this, "revealAfterMount", json.revealAfterMount);
		this.usesReadOnlyMode = new SimpleBooleanProperty(this, "usesReadOnlyMode", json.usesReadOnlyMode);
		this.mountFlags = new SimpleStringProperty(this, "mountFlags", json.mountFlags);
		this.maxCleartextFilenameLength = new SimpleIntegerProperty(this, "maxCleartextFilenameLength", json.maxCleartextFilenameLength);
		this.actionAfterUnlock = new SimpleObjectProperty<>(this, "actionAfterUnlock", json.actionAfterUnlock);
		this.autoLockWhenIdle = new SimpleBooleanProperty(this, "autoLockWhenIdle", json.autoLockWhenIdle);
		this.autoLockIdleSeconds = new SimpleIntegerProperty(this, "autoLockIdleSeconds", json.autoLockIdleSeconds);
		this.mountPoint = new SimpleObjectProperty<>(this, "mountPoint", json.mountPoint == null ? null : Path.of(json.mountPoint));
		// mount name is no longer an explicit setting, see https://github.com/cryptomator/cryptomator/pull/1318
		this.mountName = StringExpression.stringExpression(Bindings.createStringBinding(() -> {
			final String name;
			if (displayName.isEmpty().get()) {
				name = path.get().getFileName().toString();
			} else {
				name = displayName.get();
			}
			return normalizeDisplayName(name);
		}, displayName, path));

		migrateLegacySettings(json);
	}

	@SuppressWarnings("deprecation")
	private void migrateLegacySettings(VaultSettingsJson json) {
		// implicit migration of 1.6.x legacy setting "customMountPath" / "winDriveLetter":
		if (json.useCustomMountPath && !Strings.isNullOrEmpty(json.customMountPath)) {
			this.mountPoint.set(Path.of(json.customMountPath));
		} else if (!Strings.isNullOrEmpty(json.winDriveLetter)) {
			this.mountPoint.set(Path.of(json.winDriveLetter + ":\\"));
		}
	}

	Observable[] observables() {
		return new Observable[]{actionAfterUnlock, autoLockIdleSeconds, autoLockWhenIdle, displayName, maxCleartextFilenameLength, mountFlags, mountPoint, path, revealAfterMount, unlockAfterStartup, usesReadOnlyMode};
	}

	public static VaultSettings withRandomId() {
		var defaults = new VaultSettingsJson();
		defaults.id = generateId();
		return new VaultSettings(defaults);
	}

	private static String generateId() {
		byte[] randomBytes = new byte[9];
		RNG.nextBytes(randomBytes);
		return BaseEncoding.base64Url().encode(randomBytes);
	}

	public VaultSettingsJson serialized() {
		var json = new VaultSettingsJson();
		json.id = id;
		json.path = path.map(Path::toString).getValue();
		json.displayName = displayName.get();
		json.unlockAfterStartup = unlockAfterStartup.get();
		json.revealAfterMount = revealAfterMount.get();
		json.usesReadOnlyMode = usesReadOnlyMode.get();
		json.mountFlags = mountFlags.get();
		json.maxCleartextFilenameLength = maxCleartextFilenameLength.get();
		json.actionAfterUnlock = actionAfterUnlock.get();
		json.autoLockWhenIdle = autoLockWhenIdle.get();
		json.autoLockIdleSeconds = autoLockIdleSeconds.get();
		json.mountPoint = mountPoint.map(Path::toString).getValue();
		return json;
	}

	//visible for testing
	static String normalizeDisplayName(String original) {
		if (original.isBlank() || ".".equals(original) || "..".equals(original)) {
			return "_";
		}

		// replace whitespaces (tabs, linebreaks, ...) by simple space (0x20)
		var withoutFancyWhitespaces = CharMatcher.whitespace().collapseFrom(original, ' ');

		// replace control chars as well as chars that aren't allowed in file names on standard file systems by underscore
		return CharMatcher.anyOf("<>:\"/\\|?*").or(CharMatcher.javaIsoControl()).collapseFrom(withoutFancyWhitespaces, '_');
	}

	/* Getter/Setter */
	// TODO: remove accessors, make fields public

	public String getId() {
		return id;
	}

	public ObjectProperty<Path> path() {
		return path;
	}

	public StringProperty displayName() {
		return displayName;
	}

	public StringExpression mountName() {
		return mountName;
	}

	public BooleanProperty unlockAfterStartup() {
		return unlockAfterStartup;
	}

	public BooleanProperty revealAfterMount() {
		return revealAfterMount;
	}

	public Path getMountPoint() {
		return mountPoint.get();
	}

	public ObjectProperty<Path> mountPoint() {
		return mountPoint;
	}

	public BooleanProperty usesReadOnlyMode() {
		return usesReadOnlyMode;
	}

	public StringProperty mountFlags() {
		return mountFlags;
	}

	public IntegerProperty maxCleartextFilenameLength() {
		return maxCleartextFilenameLength;
	}

	public ObjectProperty<WhenUnlocked> actionAfterUnlock() {
		return actionAfterUnlock;
	}

	public WhenUnlocked getActionAfterUnlock() {
		return actionAfterUnlock.get();
	}

	public BooleanProperty autoLockWhenIdle() {
		return autoLockWhenIdle;
	}

	public IntegerProperty autoLockIdleSeconds() {
		return autoLockIdleSeconds;
	}

	/* Hashcode/Equals */

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VaultSettings other && obj.getClass().equals(this.getClass())) {
			return Objects.equals(this.id, other.id);
		} else {
			return false;
		}
	}
}
