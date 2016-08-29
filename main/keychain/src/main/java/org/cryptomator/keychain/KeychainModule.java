package org.cryptomator.keychain;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public class KeychainModule {

	@Provides
	@ElementsIntoSet
	Set<KeychainAccessStrategy> provideKeychainAccessStrategies(MacSystemKeychainAccess macKeychain, WindowsSystemKeychainAccess winKeychain) {
		return Sets.newHashSet(macKeychain, winKeychain);
	}

	@Provides
	public Optional<KeychainAccess> provideSupportedKeychain(Set<KeychainAccessStrategy> keychainAccessStrategies) {
		return keychainAccessStrategies.stream().filter(KeychainAccessStrategy::isSupported).map(KeychainAccess.class::cast).findFirst();
	}

}
