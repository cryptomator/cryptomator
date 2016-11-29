package org.cryptomator.ui.util;

import java.util.Optional;

import org.cryptomator.ui.Cryptomator;

public class ApplicationVersion {

	public static String orElse(String other) {
		return get().orElse(other);
	}

	public static Optional<String> get() {
		return Optional.ofNullable(Cryptomator.class.getPackage().getImplementationVersion());
	}

}
