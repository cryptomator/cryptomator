package org.cryptomator.common.settings;

import java.util.Arrays;

public enum VolumeImpl {
	WEBDAV("WebDAV"),
	FUSE("FUSE"),
	DOKANY("Dokany");

	private String displayName;

	VolumeImpl(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Finds a VolumeImpl by display name.
	 *
	 * @param displayName Display name of the VolumeImpl
	 * @return VolumeImpl with the given <code>displayName</code>.
	 * @throws IllegalArgumentException if not volumeImpl with the given <code>displayName</code> was found.
	 */
	public static VolumeImpl forDisplayName(String displayName) throws IllegalArgumentException {
		return Arrays.stream(values()) //
				.filter(impl -> impl.displayName.equals(displayName)) //
				.findAny() //
				.orElseThrow(IllegalArgumentException::new);
	}

}
