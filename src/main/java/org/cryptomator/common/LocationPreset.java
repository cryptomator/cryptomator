package org.cryptomator.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Enum of common cloud providers and their default local storage location path.
 */
public enum LocationPreset {

	DROPBOX("Dropbox", "~/Dropbox"),
	ICLOUD("iCloud Drive", "~/Library/Mobile Documents/com~apple~CloudDocs", "~/iCloudDrive"),
	GDRIVE("Google Drive", "~/Google Drive/My Drive", "~/Google Drive"),
	MEGA("MEGA", "~/MEGA"),
	ONEDRIVE("OneDrive", "~/OneDrive"),
	PCLOUD("pCloud", "~/pCloudDrive"),

	LOCAL("local");

	final String name;
	final List<Path> candidates;

	LocationPreset(String name, String... candidates) {
		this.name = name;

		String userHome = System.getProperty("user.home");
		this.candidates = Arrays.stream(candidates).map(c -> LocationPreset.resolveHomePath(userHome, c)).map(Path::of).toList();
	}

	private static String resolveHomePath(String home, String path) {
		if (path.startsWith("~/")) {
			return home + path.substring(1);
		} else {
			return path;
		}
	}

	/**
	 * Checks for this LocationPreset if any of the associated paths exist.
	 *
	 * @return the first existing path or null, if none exists.
	 */
	public Path existingPath() {
		for (Path candidate : candidates) {
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return name;
	}

}

