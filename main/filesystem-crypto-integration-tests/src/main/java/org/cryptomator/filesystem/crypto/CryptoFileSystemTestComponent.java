/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import javax.inject.Singleton;

import org.cryptomator.crypto.engine.impl.CryptoEngineModule;
import org.cryptomator.filesystem.shortening.ShorteningFileSystemFactory;

import dagger.Component;

/**
 * To be used in integration tests, where a {@link CryptoFileSystem} is needed in conjunction with {@link CryptoEngineTestModule} (which mocks the CSPRNG) as follows:
 * <code>
 * 	DaggerCryptoFileSystemTestComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build()
 * </code>
 */
@Singleton
@Component(modules = CryptoEngineModule.class)
public interface CryptoFileSystemTestComponent {

	CryptoFileSystemFactory cryptoFileSystemFactory();

	ShorteningFileSystemFactory shorteningFileSystemFactory();

}
