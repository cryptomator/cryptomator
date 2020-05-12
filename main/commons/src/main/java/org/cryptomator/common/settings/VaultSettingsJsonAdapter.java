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
		out.name("mountName").value(value.mountName().get());
		out.name("winDriveLetter").value(value.winDriveLetter().get());
		out.name("unlockAfterStartup").value(value.unlockAfterStartup().get());
		out.name("revealAfterMount").value(value.revealAfterMount().get());
		out.name("useCustomMountPath").value(value.useCustomMountPath().get());
		out.name("customMountPath").value(value.customMountPath().get());
		out.name("usesReadOnlyMode").value(value.usesReadOnlyMode().get());
		out.name("mountFlags").value(value.mountFlags().get());
		out.name("filenameLengthLimit").value(value.filenameLengthLimit().get());
		out.name("actionAfterUnlock").value(value.actionAfterUnlock().get().name());
		out.endObject();
	}

	public VaultSettings read(JsonReader in) throws IOException {
		String id = null;
		String path = null;
		String mountName = null;
		String customMountPath = null;
		String winDriveLetter = null;
		boolean unlockAfterStartup = VaultSettings.DEFAULT_UNLOCK_AFTER_STARTUP;
		boolean revealAfterMount = VaultSettings.DEFAULT_REAVEAL_AFTER_MOUNT;
		boolean useCustomMountPath = VaultSettings.DEFAULT_USES_INDIVIDUAL_MOUNTPATH;
		boolean usesReadOnlyMode = VaultSettings.DEFAULT_USES_READONLY_MODE;
		String mountFlags = VaultSettings.DEFAULT_MOUNT_FLAGS;
		int filenameLengthLimit = VaultSettings.DEFAULT_FILENAME_LENGTH_LIMIT;
		WhenUnlocked actionAfterUnlock = VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK;

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
				case "id" -> id = in.nextString();
				case "path" -> path = in.nextString();
				case "mountName" -> mountName = in.nextString();
				case "winDriveLetter" -> winDriveLetter = in.nextString();
				case "unlockAfterStartup" -> unlockAfterStartup = in.nextBoolean();
				case "revealAfterMount" -> revealAfterMount = in.nextBoolean();
				case "usesIndividualMountPath", "useCustomMountPath" -> useCustomMountPath = in.nextBoolean();
				case "individualMountPath", "customMountPath" -> customMountPath = in.nextString();
				case "usesReadOnlyMode" -> usesReadOnlyMode = in.nextBoolean();
				case "mountFlags" -> mountFlags = in.nextString();
				case "filenameLengthLimit" -> filenameLengthLimit = in.nextInt();
				case "actionAfterUnlock" -> actionAfterUnlock = parseActionAfterUnlock(in.nextString());
				default -> {
					LOG.warn("Unsupported vault setting found in JSON: " + name);
					in.skipValue();
				}
			}
		}
		in.endObject();

		VaultSettings vaultSettings = (id == null) ? VaultSettings.withRandomId() : new VaultSettings(id);
		vaultSettings.mountName().set(mountName);
		vaultSettings.path().set(Paths.get(path));
		vaultSettings.winDriveLetter().set(winDriveLetter);
		vaultSettings.unlockAfterStartup().set(unlockAfterStartup);
		vaultSettings.revealAfterMount().set(revealAfterMount);
		vaultSettings.useCustomMountPath().set(useCustomMountPath);
		vaultSettings.customMountPath().set(customMountPath);
		vaultSettings.usesReadOnlyMode().set(usesReadOnlyMode);
		vaultSettings.mountFlags().set(mountFlags);
		vaultSettings.filenameLengthLimit().set(filenameLengthLimit);
		vaultSettings.actionAfterUnlock().set(actionAfterUnlock);
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
