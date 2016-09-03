package org.cryptomator.keychain;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class KeychainModuleTest {

	@Test
	public void testGetKeychain() {
		Optional<KeychainAccess> keychainAccess = DaggerTestKeychainComponent.builder().jniModule(new TestJniModule()).keychainModule(new TestKeychainModule()).build().keychainAccess();
		Assert.assertTrue(keychainAccess.isPresent());
		Assert.assertTrue(keychainAccess.get() instanceof MapKeychainAccess);
	}

}
