package org.cryptomator.keychain;

import java.util.Optional;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = KeychainModule.class)
public interface KeychainComponent {

	Optional<KeychainAccess> keychainAccess();

}
