/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class KeychainModuleTest {

	@Test
	public void testGetKeychain() {
		Optional<KeychainAccess> keychainAccess = DaggerTestKeychainComponent.builder().keychainModule(new TestKeychainModule()).build().keychainAccess();
		Assertions.assertTrue(keychainAccess.isPresent());
		Assertions.assertTrue(keychainAccess.get() instanceof MapKeychainAccess);
		keychainAccess.get().storePassphrase("test", "asd");
		Assertions.assertArrayEquals("asd".toCharArray(), keychainAccess.get().loadPassphrase("test"));
	}

}
