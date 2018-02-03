package org.cryptomator.ui.model;

import org.cryptomator.cryptofs.CryptoFileSystem;

public interface NioAdapter {

	void unlock(CryptoFileSystem fs);

	void mount() throws CommandFailedException;

	void unmount() throws CommandFailedException;

	default void unmountForced() throws CommandFailedException {
		throw new CommandFailedException("Operation not supported");
	}

	void stop();

	String getFilesystemRootUrl();

	default boolean isSupported() {
		return false;
	}

	default boolean supportsForcedUnmount() {
		return false;
	}

}
