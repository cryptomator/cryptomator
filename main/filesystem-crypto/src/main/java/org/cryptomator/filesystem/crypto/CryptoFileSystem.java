/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

public class CryptoFileSystem extends CryptoFolder implements FileSystem {

	private static final String DATA_ROOT_DIR = "d";
	private static final String ROOT_DIR_FILE = "root";
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator";
	private static final String MASTERKEY_BACKUP_FILENAME = "masterkey.cryptomator.bkup";

	private final Folder physicalRoot;
	private final CryptoFileSystemDelegate delegate;

	public CryptoFileSystem(Folder physicalRoot, Cryptor cryptor, CryptoFileSystemDelegate delegate, CharSequence passphrase) throws InvalidPassphraseException {
		super(null, "", cryptor);
		this.physicalRoot = physicalRoot;
		this.delegate = delegate;
		final File masterkeyFile = physicalRoot.file(MASTERKEY_FILENAME);
		if (masterkeyFile.exists()) {
			final boolean unlocked = decryptMasterKeyFile(cryptor, masterkeyFile, passphrase);
			if (!unlocked) {
				throw new InvalidPassphraseException();
			}
		} else {
			cryptor.randomizeMasterkey();
			encryptMasterKeyFile(cryptor, masterkeyFile, passphrase);
		}
		assert masterkeyFile.exists() : "A CryptoFileSystem can not exist without a masterkey file.";
		final File backupFile = physicalRoot.file(MASTERKEY_BACKUP_FILENAME);
		masterkeyFile.copyTo(backupFile);
		create();
	}

	private static boolean decryptMasterKeyFile(Cryptor cryptor, File masterkeyFile, CharSequence passphrase) {
		try (ReadableFile file = masterkeyFile.openReadable()) {
			// TODO we need to read the whole file but can not be sure about the
			// buffer size:
			final ByteBuffer bigEnoughBuffer = ByteBuffer.allocate(500);
			file.read(bigEnoughBuffer);
			bigEnoughBuffer.flip();
			assert bigEnoughBuffer.remaining() < bigEnoughBuffer.capacity() : "The buffer wasn't big enough.";
			final byte[] fileContents = new byte[bigEnoughBuffer.remaining()];
			bigEnoughBuffer.get(fileContents);
			return cryptor.readKeysFromMasterkeyFile(fileContents, passphrase);
		}
	}

	private static void encryptMasterKeyFile(Cryptor cryptor, File masterkeyFile, CharSequence passphrase) {
		try (WritableFile file = masterkeyFile.openWritable()) {
			final byte[] fileContents = cryptor.writeKeysToMasterkeyFile(passphrase);
			file.write(ByteBuffer.wrap(fileContents));
		}
	}

	CryptoFileSystemDelegate delegate() {
		return delegate;
	}

	@Override
	protected File physicalFile() {
		return physicalDataRoot().file(ROOT_DIR_FILE);
	}

	@Override
	protected Folder physicalDataRoot() {
		return physicalRoot.folder(DATA_ROOT_DIR);
	}

	@Override
	public Optional<CryptoFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return physicalFile().exists() && physicalFolder().exists();
	}

	@Override
	public void delete() {
		// no-op.
	}

	@Override
	public void create() {
		physicalDataRoot().create();
		final File dirFile = physicalFile();
		final String directoryId = getDirectoryId();
		try (WritableFile writable = dirFile.openWritable()) {
			final ByteBuffer buf = ByteBuffer.wrap(directoryId.getBytes());
			writable.write(buf);
		}
		physicalFolder().create();
	}

	@Override
	public String toString() {
		return "/";
	}

}
