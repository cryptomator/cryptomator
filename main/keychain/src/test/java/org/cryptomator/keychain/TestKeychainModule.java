/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import java.util.Set;

import com.google.common.collect.Sets;

public class TestKeychainModule extends KeychainModule {

	@Override
	Set<KeychainAccessStrategy> provideKeychainAccessStrategies(MacSystemKeychainAccess macKeychain, WindowsProtectedKeychainAccess winKeychain) {
		return Sets.newHashSet(new MapKeychainAccess());
	}

}
