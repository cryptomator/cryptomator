package org.cryptomator.keychain;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class KeychainModuleTest {

	@Test
	public void testGetKeychain() {
		Optional<KeychainAccess> keychainAccess = DaggerKeychainComponent.builder().keychainModule(new KeychainTestModule()).build().keychainAccess();
		Assert.assertTrue(keychainAccess.isPresent());
		Assert.assertTrue(keychainAccess.get() instanceof MapKeychainAccess);
	}

}
