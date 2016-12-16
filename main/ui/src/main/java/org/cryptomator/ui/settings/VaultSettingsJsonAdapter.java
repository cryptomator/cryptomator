package org.cryptomator.ui.settings;

import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

class VaultSettingsJsonAdapter extends TypeAdapter<VaultSettings> {

	private static final Logger LOG = LoggerFactory.getLogger(VaultSettingsJsonAdapter.class);

	@Override
	public void write(JsonWriter out, VaultSettings value) throws IOException {
		out.beginObject();
		out.name("id").value(value.getId());
		out.name("path").value(value.getPath().toString());
		out.name("mountName").value(value.getMountName());
		out.name("winDriveLetter").value(value.getWinDriveLetter());
		out.endObject();
	}

	@Override
	public VaultSettings read(JsonReader in) throws IOException {
		String id = null;
		String path = null;
		String mountName = null;
		String winDriveLetter = null;

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
			default:
				LOG.warn("Unsupported vault setting found in JSON: " + name);
				in.skipValue();
			}
		}
		in.endObject();

		VaultSettings settings = (id == null) ? VaultSettings.withRandomId() : new VaultSettings(id);
		settings.setPath(Paths.get(path));
		settings.setMountName(mountName);
		settings.setWinDriveLetter(winDriveLetter);
		return settings;
	}

}
