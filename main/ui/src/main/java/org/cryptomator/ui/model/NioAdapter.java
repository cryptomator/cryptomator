package org.cryptomator.ui.model;

import org.cryptomator.cryptofs.CryptoFileSystem;

public interface NioAdapter {

	void prepare(CryptoFileSystem fs);

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
