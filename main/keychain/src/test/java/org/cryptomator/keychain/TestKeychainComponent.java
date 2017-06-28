/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import java.util.Optional;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = KeychainModule.class)
interface TestKeychainComponent {

	Optional<KeychainAccess> keychainAccess();

}
