package org.cryptomator.keychain;

import java.util.Set;

import com.google.common.collect.Sets;

public class KeychainTestModule extends KeychainModule {

	@Override
	Set<KeychainAccessStrategy> provideKeychainAccessStrategies(MacSystemKeychainAccess macKeychain, WindowsSystemKeychainAccess winKeychain) {
		return Sets.newHashSet(new MapKeychainAccess());
	}

}
