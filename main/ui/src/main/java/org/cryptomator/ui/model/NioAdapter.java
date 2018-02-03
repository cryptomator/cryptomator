package org.cryptomator.ui.model;

import org.cryptomator.cryptofs.CryptoFileSystem;

import java.util.HashMap;


public interface NioAdapter {

	void unlock(CryptoFileSystem fs);

	void mount() throws CommandFailedException;

	void unmount() throws CommandFailedException;

	void stop();

	String getFilesystemRootUrl();

	default boolean isSupported() {
		return false;
	}

	default boolean supportsForcedUnmount() {
		return false;
	}

	default void unmountForced() throws CommandFailedException {
		throw new CommandFailedException("Operation not supported");
	}
}
