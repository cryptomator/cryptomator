/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import org.cryptomator.jni.JniFunctions;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.jni.WinFunctions;

@Module
public class KeychainModule {

	@Provides
	Optional<MacFunctions> provideOptionalMacFunctions() {
		return JniFunctions.macFunctions();
	}

	@Provides
	Optional<WinFunctions> provideOptionalWinFunctions() {
		return JniFunctions.winFunctions();
	}

	@Provides
	@ElementsIntoSet
	Set<KeychainAccessStrategy> provideKeychainAccessStrategies(MacSystemKeychainAccess macKeychain, WindowsProtectedKeychainAccess winKeychain) {
		return Sets.newHashSet(macKeychain, winKeychain);
	}

	@Provides
	public Optional<KeychainAccess> provideSupportedKeychain(Set<KeychainAccessStrategy> keychainAccessStrategies) {
		return keychainAccessStrategies.stream().filter(KeychainAccessStrategy::isSupported).map(KeychainAccess.class::cast).findFirst();
	}

}
