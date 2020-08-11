/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VaultSettingsTest {

	@Test
	public void testNormalize() {
		VaultSettings settings = new VaultSettings("id");
		settings.displayName().setValue(" ");
		assertEquals("_", settings.normalizeDisplayName());

		settings.displayName().setValue("ä");
		assertEquals("a", settings.normalizeDisplayName());
		settings.displayName().setValue("Ĉ");
		assertEquals("C", settings.normalizeDisplayName());
		settings.displayName().setValue(":");
		assertEquals("_", settings.normalizeDisplayName());
		settings.displayName().setValue("汉语");
		assertEquals("_", settings.normalizeDisplayName());
	}

}
