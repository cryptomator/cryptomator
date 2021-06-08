package org.cryptomator.common.vaults;

/**
 * Enumeration used to indicate the requirements for mounting a vault
 * using a specific {@link Volume VolumeProvider}, e.g. {@link FuseVolume}.
 */
public enum MountPointRequirement {

	/**
	 * No Mountpoint on the local filesystem required. (e.g. WebDAV)
	 */
	NONE,

	/**
	 * A parent folder is required, but the actual Mountpoint must not exist.
	 */
	PARENT_NO_MOUNT_POINT,

	/**
	 * A parent folder is required, but the actual Mountpoint may exist.
	 */
	PARENT_OPT_MOUNT_POINT,

	/**
	 * The actual Mountpoint must exist and must be empty.
	 */
	EMPTY_MOUNT_POINT;
}