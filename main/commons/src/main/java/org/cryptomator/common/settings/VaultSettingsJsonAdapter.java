/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

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
		out.name("usesCustomMountPathLinux").value(value.usesCustomMountPathLinux().get());
		//TODO: should this always be written? ( because it could contain metadata, which the user may not want to save!)
		out.name("customMountPathLinux").value(value.customMountPathLinux().get());
		out.name("customMountPathMac").value(value.customMountPathMac().get());
		out.name("customMountPathWindows").value(value.customMountPathWindows().get());
		out.endObject();
	}

	public VaultSettings read(JsonReader in) throws IOException {
		String id = null;
		String path = null;
		String mountName = null;
		String individualMountPathLinux = null;
		String individualMountPathMac = null;
		String individualMountPathWindows = null;
		String winDriveLetter = null;
		boolean unlockAfterStartup = VaultSettings.DEFAULT_UNLOCK_AFTER_STARTUP;
		boolean revealAfterMount = VaultSettings.DEFAULT_REAVEAL_AFTER_MOUNT;
		boolean usesIndividualMountPathLinux = VaultSettings.DEFAULT_USES_CUSTOM_MOUNTPATH;
		boolean usesIndividualMountPathMac = VaultSettings.DEFAULT_USES_CUSTOM_MOUNTPATH;
		boolean usesIndividualMountPathWindows = VaultSettings.DEFAULT_USES_CUSTOM_MOUNTPATH;

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
				case "usesCustomMountPathLinux":
					usesIndividualMountPathLinux = in.nextBoolean();
					break;
				case "customMountPathLinux":
					individualMountPathLinux = in.nextString();
					break;
				case "usesCustomMountPathMac":
					usesIndividualMountPathMac = in.nextBoolean();
					break;
				case "customMountPathMac":
					individualMountPathMac = in.nextString();
					break;
				case "usesCustomMountPathWindows":
					usesIndividualMountPathWindows = in.nextBoolean();
					break;
				case "customMountPathWindows":
					individualMountPathWindows = in.nextString();
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
		vaultSettings.usesCustomMountPathLinux().set(usesIndividualMountPathLinux);
		vaultSettings.customMountPathLinux().set(individualMountPathLinux);
		vaultSettings.usesCustomMountPathMac().set(usesIndividualMountPathMac);
		vaultSettings.customMountPathMac().set(individualMountPathMac);
		vaultSettings.usesCustomMountPathWindows().set(usesIndividualMountPathWindows);
		vaultSettings.customMountPathWindows().set(individualMountPathWindows);
		return vaultSettings;
	}

}
