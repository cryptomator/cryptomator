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
	ICLOUDDRIVE("iCloud Drive", "~/Library/Mobile Documents/com~apple~CloudDocs", "~/iCloudDrive"),
	GDRIVE("Google Drive", "~/Google Drive/My Drive", "~/Google Drive"),
	MEGA("MEGA", "~/MEGA"),
	ONEDRIVE("OneDrive", "~/OneDrive"),
	PCLOUD("pCloud", "~/pCloudDrive"),

	LOCAL("local");

	private final String name;
	private final List<Path> candidates;

	LocationPreset(String name, String... candidates) {
		this.name = name;
		this.candidates = Arrays.stream(candidates).map(UserHome::resolve).map(Path::of).toList();
	}

	/**
	 * Checks for this LocationPreset if any of the associated paths exist.
	 *
	 * @return the first existing path or null, if none exists.
	 */
	public Path existingPath() {
		return candidates.stream().filter(Files::isDirectory).findFirst().orElse(null);
	}

	public String getDisplayName() {
		return name;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

	//this contruct is needed, since static members are initialized after every enum member is initialized
	//TODO: refactor this to normal class and use this also in different parts of the project
	private static class UserHome {

		private static final String USER_HOME = System.getProperty("user.home");

		private static String resolve(String path) {
			if (path.startsWith("~/")) {
				return UserHome.USER_HOME + path.substring(1);
			} else {
				return path;
			}
		}
	}

}

