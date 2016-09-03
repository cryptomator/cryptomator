package org.cryptomator.keychain;

import java.util.Optional;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = KeychainModule.class)
interface TestKeychainComponent {

	Optional<KeychainAccess> keychainAccess();

}
