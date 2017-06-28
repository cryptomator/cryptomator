/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VaultSettingsTest {

	@Test
	public void testNormalize() throws Exception {
		assertEquals("_", VaultSettings.normalizeMountName(" "));
		assertEquals("a", VaultSettings.normalizeMountName("ä"));
		assertEquals("C", VaultSettings.normalizeMountName("Ĉ"));
		assertEquals("_", VaultSettings.normalizeMountName(":"));
		assertEquals("_", VaultSettings.normalizeMountName("汉语"));
	}

}
