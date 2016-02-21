/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoEngineTestModule;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.crypto.CryptoFileSystemTestComponent;
import org.cryptomator.filesystem.crypto.DaggerCryptoFileSystemTestComponent;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.mockito.Mockito;

public class InMemoryWebDavServer {

	private static final CryptoFileSystemTestComponent CRYPTO_FS_COMP = DaggerCryptoFileSystemTestComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build();

	public static void main(String[] args) throws Exception {
		new FileSystemWebDavServer(cryptoFileSystem()).run();
	}

	private static FileSystem cryptoFileSystem() {
		FileSystem shorteningFileSystem = shorteningFileSystem();
		CRYPTO_FS_COMP.cryptoFileSystemFactory().initializeNew(shorteningFileSystem, "asd");
		return CRYPTO_FS_COMP.cryptoFileSystemFactory().unlockExisting(shorteningFileSystem, "asd", Mockito.mock(CryptoFileSystemDelegate.class));
	}

	private static FileSystem shorteningFileSystem() {
		return CRYPTO_FS_COMP.shorteningFileSystemFactory().get(inMemoryFileSystem());
	}

	private static FileSystem inMemoryFileSystem() {
		return new InMemoryFileSystem();
	}

}
