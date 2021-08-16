/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

class VaultSettingsJsonAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(VaultSettingsJsonAdapter.class);

	public void write(JsonWriter out, VaultSettings value) throws IOException {
		out.beginObject();
		out.name("id").value(value.getId());
		out.name("path").value(value.path().get().toString());
		out.name("displayName").value(value.displayName().get());
		out.name("winDriveLetter").value(value.winDriveLetter().get());
		out.name("unlockAfterStartup").value(value.unlockAfterStartup().get());
		out.name("revealAfterMount").value(value.revealAfterMount().get());
		out.name("useCustomMountPath").value(value.useCustomMountPath().get());
		out.name("customMountPath").value(value.customMountPath().get());
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
		String customMountPath = null;
		String winDriveLetter = null;
		boolean unlockAfterStartup = VaultSettings.DEFAULT_UNLOCK_AFTER_STARTUP;
		boolean revealAfterMount = VaultSettings.DEFAULT_REVEAL_AFTER_MOUNT;
		boolean useCustomMountPath = VaultSettings.DEFAULT_USES_INDIVIDUAL_MOUNTPATH;
		boolean usesReadOnlyMode = VaultSettings.DEFAULT_USES_READONLY_MODE;
		String mountFlags = VaultSettings.DEFAULT_MOUNT_FLAGS;
		int maxCleartextFilenameLength = VaultSettings.DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH;
		WhenUnlocked actionAfterUnlock = VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK;
		boolean autoLockWhenIdle = VaultSettings.DEFAULT_AUTOLOCK_WHEN_IDLE;
		int autoLockIdleSeconds = VaultSettings.DEFAULT_AUTOLOCK_IDLE_SECONDS;

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
				case "id" -> id = in.nextString();
				case "path" -> path = in.nextString();
				case "mountName" -> mountName = in.nextString(); //see https://github.com/cryptomator/cryptomator/pull/1318
				case "displayName" -> displayName = in.nextString();
				case "winDriveLetter" -> winDriveLetter = in.nextString();
				case "unlockAfterStartup" -> unlockAfterStartup = in.nextBoolean();
				case "revealAfterMount" -> revealAfterMount = in.nextBoolean();
				case "usesIndividualMountPath", "useCustomMountPath" -> useCustomMountPath = in.nextBoolean();
				case "individualMountPath", "customMountPath" -> customMountPath = in.nextString();
				case "usesReadOnlyMode" -> usesReadOnlyMode = in.nextBoolean();
				case "mountFlags" -> mountFlags = in.nextString();
				case "maxCleartextFilenameLength" -> maxCleartextFilenameLength = in.nextInt();
				case "actionAfterUnlock" -> actionAfterUnlock = parseActionAfterUnlock(in.nextString());
				case "autoLockWhenIdle" -> autoLockWhenIdle = in.nextBoolean();
				case "autoLockIdleSeconds" -> autoLockIdleSeconds = in.nextInt();
				default -> {
					LOG.warn("Unsupported vault setting found in JSON: " + name);
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
		vaultSettings.winDriveLetter().set(winDriveLetter);
		vaultSettings.unlockAfterStartup().set(unlockAfterStartup);
		vaultSettings.revealAfterMount().set(revealAfterMount);
		vaultSettings.useCustomMountPath().set(useCustomMountPath);
		vaultSettings.customMountPath().set(customMountPath);
		vaultSettings.usesReadOnlyMode().set(usesReadOnlyMode);
		vaultSettings.mountFlags().set(mountFlags);
		vaultSettings.maxCleartextFilenameLength().set(maxCleartextFilenameLength);
		vaultSettings.actionAfterUnlock().set(actionAfterUnlock);
		vaultSettings.autoLockWhenIdle().set(autoLockWhenIdle);
		vaultSettings.autoLockIdleSeconds().set(autoLockIdleSeconds);
		return vaultSettings;
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
