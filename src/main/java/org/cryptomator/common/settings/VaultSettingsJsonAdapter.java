/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

class VaultSettingsJsonAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(VaultSettingsJsonAdapter.class);

	public void write(JsonWriter out, VaultSettings value) throws IOException {
		out.beginObject();
		out.name("id").value(value.getId());
		out.name("path").value(value.path().get().toString());
		out.name("displayName").value(value.displayName().get());
		out.name("unlockAfterStartup").value(value.unlockAfterStartup().get());
		out.name("revealAfterMount").value(value.revealAfterMount().get());
		var mountPoint = value.mountPoint().get();
		out.name("mountPoint").value(mountPoint != null ? mountPoint.toAbsolutePath().toString() : null);
		out.name("usesReadOnlyMode").value(value.usesReadOnlyMode().get());
		out.name("mountFlags").value(value.mountFlags().get());
		out.name("maxCleartextFilenameLength").value(value.maxCleartextFilenameLength().get());
		out.name("actionAfterUnlock").value(value.actionAfterUnlock().get().name());
		out.name("autoLockWhenIdle").value(value.autoLockWhenIdle().get());
		out.name("autoLockIdleSeconds").value(value.autoLockIdleSeconds().get());
		out.endObject();
	}

	public VaultSettings read(JsonReader in) throws IOException {
		String id = null;
		String path = null;
		String mountName = null; //see https://github.com/cryptomator/cryptomator/pull/1318
		String displayName = null;
		boolean unlockAfterStartup = VaultSettings.DEFAULT_UNLOCK_AFTER_STARTUP;
		boolean revealAfterMount = VaultSettings.DEFAULT_REVEAL_AFTER_MOUNT;
		boolean usesReadOnlyMode = VaultSettings.DEFAULT_USES_READONLY_MODE;
		String mountFlags = VaultSettings.DEFAULT_MOUNT_FLAGS;
		Path mountPoint = null;
		int maxCleartextFilenameLength = VaultSettings.DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH;
		WhenUnlocked actionAfterUnlock = VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK;
		boolean autoLockWhenIdle = VaultSettings.DEFAULT_AUTOLOCK_WHEN_IDLE;
		int autoLockIdleSeconds = VaultSettings.DEFAULT_AUTOLOCK_IDLE_SECONDS;

		//legacy from 1.6.x
		boolean useCustomMountPath = false;
		String customMountPath = "";
		String winDriveLetter = "";
		//legacy end

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
				case "id" -> id = in.nextString();
				case "path" -> path = in.nextString();
				case "mountName" -> mountName = in.nextString(); //see https://github.com/cryptomator/cryptomator/pull/1318
				case "displayName" -> displayName = in.nextString();
				case "unlockAfterStartup" -> unlockAfterStartup = in.nextBoolean();
				case "revealAfterMount" -> revealAfterMount = in.nextBoolean();
				case "usesReadOnlyMode" -> usesReadOnlyMode = in.nextBoolean();
				case "mountFlags" -> mountFlags = in.nextString();
				case "mountPoint" -> {
					if (JsonToken.NULL == in.peek()) {
						in.nextNull();
					} else {
						mountPoint = parseMountPoint(in.nextString());
					}
				}
				case "maxCleartextFilenameLength" -> maxCleartextFilenameLength = in.nextInt();
				case "actionAfterUnlock" -> actionAfterUnlock = parseActionAfterUnlock(in.nextString());
				case "autoLockWhenIdle" -> autoLockWhenIdle = in.nextBoolean();
				case "autoLockIdleSeconds" -> autoLockIdleSeconds = in.nextInt();
				//legacy from 1.6.x
				case "winDriveLetter" -> winDriveLetter = in.nextString();
				case "usesIndividualMountPath", "useCustomMountPath" -> useCustomMountPath = in.nextBoolean();
				case "individualMountPath", "customMountPath" -> customMountPath = in.nextString();
				//legacy end
				default -> {
					LOG.warn("Unsupported vault setting found in JSON: {}", name);
					in.skipValue();
				}
			}
		}
		in.endObject();

		VaultSettings vaultSettings = (id == null) ? VaultSettings.withRandomId() : new VaultSettings(id);
		if (displayName != null) { //see https://github.com/cryptomator/cryptomator/pull/1318
			vaultSettings.displayName().set(displayName);
		} else {
			vaultSettings.displayName().set(mountName);
		}
		vaultSettings.path().set(Paths.get(path));
		vaultSettings.unlockAfterStartup().set(unlockAfterStartup);
		vaultSettings.revealAfterMount().set(revealAfterMount);
		vaultSettings.usesReadOnlyMode().set(usesReadOnlyMode);
		vaultSettings.mountFlags().set(mountFlags);
		vaultSettings.maxCleartextFilenameLength().set(maxCleartextFilenameLength);
		vaultSettings.actionAfterUnlock().set(actionAfterUnlock);
		vaultSettings.autoLockWhenIdle().set(autoLockWhenIdle);
		vaultSettings.autoLockIdleSeconds().set(autoLockIdleSeconds);
		vaultSettings.mountPoint().set(mountPoint);
		//legacy from 1.6.x
		if(useCustomMountPath && !customMountPath.isBlank()) {
			vaultSettings.mountPoint().set(parseMountPoint(customMountPath));
		} else if(!winDriveLetter.isBlank() ) {
			vaultSettings.mountPoint().set(parseMountPoint(winDriveLetter+":\\"));
		}
		//legacy end
		return vaultSettings;
	}

	private Path parseMountPoint(String mountPoint) {
		try {
			return Path.of(mountPoint);
		} catch (InvalidPathException e) {
			LOG.warn("Invalid string as mount point. Defaulting to null.");
			return null;
		}
	}

	private WhenUnlocked parseActionAfterUnlock(String actionAfterUnlockName) {
		try {
			return WhenUnlocked.valueOf(actionAfterUnlockName.toUpperCase());
		} catch (IllegalArgumentException e) {
			LOG.warn("Invalid action after unlock {}. Defaulting to {}.", actionAfterUnlockName, VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK);
			return VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK;
		}
	}

}
