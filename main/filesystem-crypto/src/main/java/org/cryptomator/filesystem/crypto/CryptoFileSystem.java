/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
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
import org.cryptomator.filesystem.WritableFile;

public class CryptoFileSystem extends CryptoFolder implements FileSystem {

	private static final String DATA_ROOT_DIR = "d";
	private static final String ROOT_DIR_FILE = "root";

	private final Folder physicalRoot;
	private final CryptoFileSystemDelegate delegate;

	public CryptoFileSystem(Folder physicalRoot, Cryptor cryptor, CryptoFileSystemDelegate delegate, CharSequence passphrase) throws InvalidPassphraseException {
		super(null, "", cryptor);
		if (cryptor.isDestroyed()) {
			throw new IllegalArgumentException("Cryptor's keys must not be destroyed.");
		}
		this.physicalRoot = physicalRoot;
		this.delegate = delegate;
		create();
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
