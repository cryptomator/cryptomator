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
		out.name("usesCustomMountPath").value(value.usesCustomMountPath().get());
		out.name("customMountPath").value(value.customMountPath().get());    //TODO: should this always be written? ( because it could contain metadata, which the user may not want to save!)
		out.endObject();
	}

	public VaultSettings read(JsonReader in) throws IOException {
		String id = null;
		String path = null;
		String mountName = null;
		String individualMountPath = null;
		String winDriveLetter = null;
		boolean unlockAfterStartup = VaultSettings.DEFAULT_UNLOCK_AFTER_STARTUP;
		boolean revealAfterMount = VaultSettings.DEFAULT_REAVEAL_AFTER_MOUNT;
		boolean usesIndividualMountPath = VaultSettings.DEFAULT_USES_CUSTOM_MOUNTPATH;

		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			switch (name) {
				case "id":
					id = in.nextString();
					break;
				case "path":
					path = in.nextString();
					break;
				case "mountName":
					mountName = in.nextString();
					break;
				case "winDriveLetter":
					winDriveLetter = in.nextString();
					break;
				case "unlockAfterStartup":
					unlockAfterStartup = in.nextBoolean();
					break;
				case "revealAfterMount":
					revealAfterMount = in.nextBoolean();
					break;
				case "usesCustomMountPath":
					usesIndividualMountPath = in.nextBoolean();
					break;
				case "customMountPath":
					individualMountPath = in.nextString();
					break;
				default:
					LOG.warn("Unsupported vault setting found in JSON: " + name);
					in.skipValue();
			}
		}
		in.endObject();

		VaultSettings vaultSettings = (id == null) ? VaultSettings.withRandomId() : new VaultSettings(id);
		vaultSettings.mountName().set(mountName);
		vaultSettings.path().set(Paths.get(path));
		vaultSettings.winDriveLetter().set(winDriveLetter);
		vaultSettings.unlockAfterStartup().set(unlockAfterStartup);
		vaultSettings.revealAfterMount().set(revealAfterMount);
		vaultSettings.usesCustomMountPath().set(usesIndividualMountPath);
		vaultSettings.customMountPath().set(individualMountPath);
		return vaultSettings;
	}

}
