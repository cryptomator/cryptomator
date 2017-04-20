/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
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
