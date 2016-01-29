/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VaultTest {

	@Test
	public void testNormalize() throws Exception {
		assertEquals("_", Vault.normalize(" "));
		assertEquals("a", Vault.normalize("ä"));
		assertEquals("C", Vault.normalize("Ĉ"));
		assertEquals("_", Vault.normalize(":"));
		assertEquals("", Vault.normalize("汉语"));
	}

}
