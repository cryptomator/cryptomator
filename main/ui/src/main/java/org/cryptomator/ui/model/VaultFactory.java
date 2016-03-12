/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.filesystem.crypto.CryptoFileSystemFactory;
import org.cryptomator.filesystem.shortening.ShorteningFileSystemFactory;
import org.cryptomator.ui.util.DeferredCloser;

@Singleton
public class VaultFactory {

	private final ShorteningFileSystemFactory shorteningFileSystemFactory;
	private final CryptoFileSystemFactory cryptoFileSystemFactory;
	private final DeferredCloser closer;

	@Inject
	public VaultFactory(ShorteningFileSystemFactory shorteningFileSystemFactory, CryptoFileSystemFactory cryptoFileSystemFactory, DeferredCloser closer) {
		this.shorteningFileSystemFactory = shorteningFileSystemFactory;
		this.cryptoFileSystemFactory = cryptoFileSystemFactory;
		this.closer = closer;
	}

	public Vault createVault(Path path) {
		return new Vault(path, shorteningFileSystemFactory, cryptoFileSystemFactory, closer);
	}

}
