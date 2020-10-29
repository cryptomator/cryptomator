package org.cryptomator.common.vaults;

import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptofs.CryptoFileSystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Takes a Volume and usess it to mount an unlocked vault
 */
public interface Volume {

	/**
	 * Checks in constant time whether this volume type is supported on the system running Cryptomator.
	 *
	 * @return true if this volume can be mounted
	 */
	boolean isSupported();

	/**
	 * Gets the coresponding enum type of the {@link VolumeImpl volume implementation ("VolumeImpl")} that is implemented by this Volume.
	 *
	 * @return the type of implementation as defined by the {@link VolumeImpl VolumeImpl enum}
	 */
	VolumeImpl getImplementationType();

	/**
	 * @param fs
	 * @throws IOException
	 */
	void mount(CryptoFileSystem fs, String mountFlags) throws IOException, VolumeException, InvalidMountPointException;

	void reveal() throws VolumeException;

	void unmount() throws VolumeException;

	Optional<Path> getMountPoint();

	MountPointRequirement getMountPointRequirement();

	// optional forced unmounting:

	default boolean supportsForcedUnmount() {
		return false;
	}

	default void unmountForced() throws VolumeException {
		throw new VolumeException("Operation not supported.");
	}

	static VolumeImpl[] getCurrentSupportedAdapters() {
		return Stream.of(VolumeImpl.values()).filter(impl -> switch (impl) {
			case WEBDAV -> WebDavVolume.isSupportedStatic();
			case DOKANY -> DokanyVolume.isSupportedStatic();
			case FUSE -> FuseVolume.isSupportedStatic();
		}).toArray(VolumeImpl[]::new);
	}

	/**
	 * Exception thrown when a volume-specific command such as mount/unmount/reveal failed.
	 */
	class VolumeException extends Exception {

		public VolumeException(String message) {
			super(message);
		}

		public VolumeException(Throwable cause) {
			super(cause);
		}

		public VolumeException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
