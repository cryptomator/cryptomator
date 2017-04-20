/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.util.Optional;

public class ApplicationVersion {

	public static String orElse(String other) {
		return get().orElse(other);
	}

	public static Optional<String> get() {
		return Optional.ofNullable(Cryptomator.class.getPackage().getImplementationVersion());
	}

}
