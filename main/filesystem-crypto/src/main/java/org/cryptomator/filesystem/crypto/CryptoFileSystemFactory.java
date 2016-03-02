/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.crypto.Constants.MASTERKEY_FILENAME;

import java.io.UncheckedIOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

@Singleton
public class CryptoFileSystemFactory {

	private final Masterkeys masterkeys;
	private final BlockAlignedFileSystemFactory blockAlignedFileSystemFactory;

	@Inject
	public CryptoFileSystemFactory(Masterkeys masterkeys, BlockAlignedFileSystemFactory blockAlignedFileSystemFactory) {
		this.masterkeys = masterkeys;
		this.blockAlignedFileSystemFactory = blockAlignedFileSystemFactory;
	}

	public boolean isValidVaultStructure(Folder vaultLocation) {
		return vaultLocation.file(MASTERKEY_FILENAME).exists();
	}

	public void initializeNew(Folder vaultLocation, CharSequence passphrase) {
		masterkeys.initialize(vaultLocation, passphrase);
	}

	public FileSystem unlockExisting(Folder vaultLocation, CharSequence passphrase, CryptoFileSystemDelegate delegate) throws InvalidPassphraseException {
		final Cryptor cryptor = masterkeys.decrypt(vaultLocation, passphrase);
		masterkeys.backup(vaultLocation);
		final FileSystem cryptoFs = new CryptoFileSystem(vaultLocation, cryptor, delegate, passphrase);
		return blockAlignedFileSystemFactory.get(cryptoFs);
	}

	public void changePassphrase(Folder vaultLocation, CharSequence oldPassphrase, CharSequence newPassphrase) throws InvalidPassphraseException {
		masterkeys.backup(vaultLocation);
		try {
			masterkeys.changePassphrase(vaultLocation, oldPassphrase, newPassphrase);
			// At this point the backup is still using the old password.
			// It will be changed as soon as the user unlocks the vault the next time.
			// This way he can still restore the old password, if he doesn't remember the new one.
		} catch (UncheckedIOException e) {
			masterkeys.restoreBackup(vaultLocation);
		}
	}
}
