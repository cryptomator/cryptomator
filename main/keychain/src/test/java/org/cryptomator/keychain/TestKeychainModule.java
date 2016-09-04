package org.cryptomator.keychain;

import java.util.Set;

import com.google.common.collect.Sets;

public class TestKeychainModule extends KeychainModule {

	@Override
	Set<KeychainAccessStrategy> provideKeychainAccessStrategies(MacSystemKeychainAccess macKeychain, WindowsProtectedKeychainAccess winKeychain) {
		return Sets.newHashSet(new MapKeychainAccess());
	}

}
