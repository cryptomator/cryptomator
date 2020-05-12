package org.cryptomator.common.settings;

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

}
