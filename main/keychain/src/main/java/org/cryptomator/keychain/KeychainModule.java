/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import org.cryptomator.common.JniModule;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;

@Module(includes = {JniModule.class})
public abstract class KeychainModule {

	@Binds
	@IntoSet
	abstract KeychainAccessStrategy bindMacSystemKeychainAccess(MacSystemKeychainAccess keychainAccessStrategy);

	@Binds
	@IntoSet
	abstract KeychainAccessStrategy bindWindowsProtectedKeychainAccess(WindowsProtectedKeychainAccess keychainAccessStrategy);
	
	@Binds
	@IntoSet
	abstract KeychainAccessStrategy bindLinuxSystemKeychainAccess(LinuxSystemKeychainAccess keychainAccessStrategy);

	@Provides
	@Singleton
	static Optional<KeychainAccessStrategy> provideSupportedKeychain(Set<KeychainAccessStrategy> keychainAccessStrategies) {
		return keychainAccessStrategies.stream().filter(KeychainAccessStrategy::isSupported).findFirst();
	}
	
	@Provides
	@Singleton
	public static Optional<KeychainManager> provideKeychainManager(Optional<KeychainAccessStrategy> keychainAccess) {
		return keychainAccess.map(KeychainManager::new);
	}

}
