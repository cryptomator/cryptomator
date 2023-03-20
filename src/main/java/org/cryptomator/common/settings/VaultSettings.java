/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.common.base.CharMatcher;
import com.google.common.io.BaseEncoding;

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
import java.util.Objects;
import java.util.Random;

/**
 * The settings specific to a single vault.
 */
public class VaultSettings {

	public static final boolean DEFAULT_UNLOCK_AFTER_STARTUP = false;
	public static final boolean DEFAULT_REVEAL_AFTER_MOUNT = true;
	public static final boolean DEFAULT_USES_READONLY_MODE = false;
	public static final String DEFAULT_MOUNT_FLAGS = "";
	public static final int DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH = -1;
	public static final WhenUnlocked DEFAULT_ACTION_AFTER_UNLOCK = WhenUnlocked.ASK;
	public static final boolean DEFAULT_AUTOLOCK_WHEN_IDLE = false;
	public static final int DEFAULT_AUTOLOCK_IDLE_SECONDS = 30 * 60;

	private static final Random RNG = new Random();

	private final String id;
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	private final StringProperty displayName = new SimpleStringProperty();
	private final BooleanProperty vaultIdAsVolumeId = new SimpleBooleanProperty(false);
	private final BooleanProperty unlockAfterStartup = new SimpleBooleanProperty(DEFAULT_UNLOCK_AFTER_STARTUP);
	private final BooleanProperty revealAfterMount = new SimpleBooleanProperty(DEFAULT_REVEAL_AFTER_MOUNT);
	private final BooleanProperty usesReadOnlyMode = new SimpleBooleanProperty(DEFAULT_USES_READONLY_MODE);
	private final StringProperty mountFlags = new SimpleStringProperty(DEFAULT_MOUNT_FLAGS); //TODO: remove empty default mount flags and let this property be null if not used
	private final IntegerProperty maxCleartextFilenameLength = new SimpleIntegerProperty(DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH);
	private final ObjectProperty<WhenUnlocked> actionAfterUnlock = new SimpleObjectProperty<>(DEFAULT_ACTION_AFTER_UNLOCK);
	private final BooleanProperty autoLockWhenIdle = new SimpleBooleanProperty(DEFAULT_AUTOLOCK_WHEN_IDLE);
	private final IntegerProperty autoLockIdleSeconds = new SimpleIntegerProperty(DEFAULT_AUTOLOCK_IDLE_SECONDS);
	private final StringExpression mountName;
	private final ObjectProperty<Path> mountPoint = new SimpleObjectProperty<>();

	public VaultSettings(String id) {
		this.id = Objects.requireNonNull(id);
		this.mountName = StringExpression.stringExpression(Bindings.createStringBinding(() -> {
			final String name;
			if (displayName.isEmpty().get()) {
				name = path.get().getFileName().toString();
			} else {
				name = displayName.get();
			}
			return normalizeDisplayName(name);
		}, displayName, path));
	}

	Observable[] observables() {
		return new Observable[]{actionAfterUnlock, autoLockIdleSeconds, autoLockWhenIdle, displayName, maxCleartextFilenameLength, mountFlags, mountPoint, path, revealAfterMount, unlockAfterStartup, usesReadOnlyMode};
	}

	public static VaultSettings withRandomId() {
		return new VaultSettings(generateId());
	}

	private static String generateId() {
		byte[] randomBytes = new byte[9];
		RNG.nextBytes(randomBytes);
		return BaseEncoding.base64Url().encode(randomBytes);
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

	public BooleanProperty usesVaultIdAsVolumeId() {
		return vaultIdAsVolumeId;
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
