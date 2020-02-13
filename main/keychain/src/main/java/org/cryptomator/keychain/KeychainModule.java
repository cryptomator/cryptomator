/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import com.google.common.collect.Sets;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import org.cryptomator.common.JniModule;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;

@Module(includes = {JniModule.class})
public class KeychainModule {

	@Provides
	@ElementsIntoSet
	Set<KeychainAccessStrategy> provideKeychainAccessStrategies(MacSystemKeychainAccess macKeychain, WindowsProtectedKeychainAccess winKeychain, LinuxSecretServiceKeychainAccess linKeychain) {
		return Sets.newHashSet(macKeychain, winKeychain, linKeychain);
	}

	@Provides
	@Singleton
	public Optional<KeychainAccess> provideSupportedKeychain(Set<KeychainAccessStrategy> keychainAccessStrategies) {
		return keychainAccessStrategies.stream().filter(KeychainAccessStrategy::isSupported).map(KeychainAccess.class::cast).findFirst();
	}

}
