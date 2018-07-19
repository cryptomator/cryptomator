package org.cryptomator.ui.model;

import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptofs.CryptoFileSystem;

import java.io.IOException;
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
	 * @param fs
	 * @throws IOException
	 */
	void mount(CryptoFileSystem fs) throws IOException, VolumeException;

	void reveal() throws VolumeException;

	void unmount() throws VolumeException;

	// optional forced unmounting:

	default boolean supportsForcedUnmount() {
		return false;
	}

	default void unmountForced() throws VolumeException {
		throw new VolumeException("Operation not supported.");
	}

	static VolumeImpl[] getCurrentSupportedAdapters() {
		return Stream.of(VolumeImpl.values()).filter(impl -> {
			switch (impl) {
				case WEBDAV:
					return WebDavVolume.isSupportedStatic();
				case DOKANY:
					return DokanyVolume.isSupportedStatic();
				case FUSE:
					return FuseVolume.isSupportedStatic();
				default:
					return false;//throw new IllegalStateException("Adapter not implemented.");
			}
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
