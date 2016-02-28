/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.crypto.Constants.MASTERKEY_BACKUP_FILENAME;
import static org.cryptomator.filesystem.crypto.Constants.MASTERKEY_FILENAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.WritableFile;

@Singleton
class Masterkeys {

	private final Provider<Cryptor> cryptorProvider;

	@Inject
	public Masterkeys(Provider<Cryptor> cryptorProvider) {
		this.cryptorProvider = cryptorProvider;
	}

	public void initialize(Folder vaultLocation, CharSequence passphrase) {
		File masterkeyFile = vaultLocation.file(MASTERKEY_FILENAME);
		Cryptor cryptor = cryptorProvider.get();
		try {
			cryptor.randomizeMasterkey();
			writeMasterKey(masterkeyFile, cryptor, passphrase);
		} finally {
			cryptor.destroy();
		}
	}

	public Cryptor decrypt(Folder vaultLocation, CharSequence passphrase) throws InvalidPassphraseException {
		File masterkeyFile = vaultLocation.file(MASTERKEY_FILENAME);
		Cryptor cryptor = cryptorProvider.get();
		try {
			readMasterKey(masterkeyFile, cryptor, passphrase);
		} catch (UncheckedIOException e) {
			cryptor.destroy();
		}
		return cryptor;
	}

	public void changePassphrase(Folder vaultLocation, CharSequence oldPassphrase, CharSequence newPassphrase) throws InvalidPassphraseException {
		File masterkeyFile = vaultLocation.file(MASTERKEY_FILENAME);
		Cryptor cryptor = cryptorProvider.get();
		try {
			readMasterKey(masterkeyFile, cryptor, oldPassphrase);
			writeMasterKey(masterkeyFile, cryptor, newPassphrase);
		} finally {
			cryptor.destroy();
		}
	}

	public void backup(Folder vaultLocation) {
		File masterkeyFile = vaultLocation.file(MASTERKEY_FILENAME);
		File backupFile = vaultLocation.file(MASTERKEY_BACKUP_FILENAME);
		masterkeyFile.copyTo(backupFile);
	}

	public void restoreBackup(Folder vaultLocation) {
		File backupFile = vaultLocation.file(MASTERKEY_BACKUP_FILENAME);
		File masterkeyFile = vaultLocation.file(MASTERKEY_FILENAME);
		backupFile.copyTo(masterkeyFile);
	}

	/* I/O */

	private static void readMasterKey(File file, Cryptor cryptor, CharSequence passphrase) throws UncheckedIOException, InvalidPassphraseException {
		try (InputStream in = Channels.newInputStream(file.openReadable())) {
			final byte[] fileContents = IOUtils.toByteArray(in);
			cryptor.readKeysFromMasterkeyFile(fileContents, passphrase);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void writeMasterKey(File file, Cryptor cryptor, CharSequence passphrase) throws UncheckedIOException {
		try (WritableFile writable = file.openWritable()) {
			final byte[] fileContents = cryptor.writeKeysToMasterkeyFile(passphrase);
			writable.write(ByteBuffer.wrap(fileContents));
		}
	}

}
