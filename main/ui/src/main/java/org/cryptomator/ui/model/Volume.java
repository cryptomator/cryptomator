package org.cryptomator.ui.model;

import org.cryptomator.cryptofs.CryptoFileSystem;

import java.io.IOException;

/**
 * Takes a Volume and usess it to mount an unlocked vault
 */
public interface Volume {

	void prepare(CryptoFileSystem fs) throws IOException;

	void mount() throws CommandFailedException;

	default void reveal() throws CommandFailedException {
		throw new CommandFailedException("Not implemented.");
	}

	void unmount() throws CommandFailedException;

	default void unmountForced() throws CommandFailedException {
		throw new CommandFailedException("Operation not supported.");
	}

	void stop();

	String getMountUri();

	default boolean isSupported() {
		return false;
	}

	default boolean supportsForcedUnmount() {
		return false;
	}

}
