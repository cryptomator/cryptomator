/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import org.cryptomator.crypto.engine.impl.CryptoEngineModule;

import dagger.Module;

@Module(includes = CryptoEngineModule.class)
public class CryptoFileSystemModule {

}
