package org.cryptomator.ui.model;

import org.cryptomator.cryptofs.CryptoFileSystem;

import java.io.IOException;

/**
 * Takes a Volume and usess it to mount an unlocked vault
 */
public interface Volume {

	/**
	 * Checks in constant time whether this volume type is supported on the system running Cryptomator.
	 * @return true if this volume can be mounted
	 */
	boolean isSupported();

	/**
	 *
	 * @param fs
	 * @throws IOException
	 */
	void mount(CryptoFileSystem fs) throws IOException, CommandFailedException;

	default void reveal() throws CommandFailedException {
		throw new CommandFailedException("Not implemented.");
	}

	void unmount() throws CommandFailedException;

	// optional forced unmounting:

	default boolean supportsForcedUnmount() {
		return false;
	}

	default void unmountForced() throws CommandFailedException {
		throw new CommandFailedException("Operation not supported.");
	}

}
