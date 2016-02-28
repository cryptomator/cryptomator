/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.crypto.Constants.DATA_ROOT_DIR;
import static org.cryptomator.filesystem.crypto.Constants.ROOT_DIRECOTRY_ID;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

class CryptoFileSystem extends CryptoFolder implements FileSystem {

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
	protected Optional<String> getDirectoryId() {
		return Optional.of(ROOT_DIRECOTRY_ID);
	}

	@Override
	protected Optional<File> physicalFile() {
		throw new UnsupportedOperationException("Crypto filesystem root doesn't provide a directory file, as the directory ID is fixed.");
	}

	@Override
	protected Folder physicalDataRoot() {
		return physicalRoot.folder(DATA_ROOT_DIR);
	}

	@Override
	public Optional<Long> quotaUsedBytes() {
		return physicalRoot.fileSystem().quotaUsedBytes();
	}

	@Override
	public Optional<Long> quotaAvailableBytes() {
		return physicalRoot.fileSystem().quotaAvailableBytes();
	}

	@Override
	public Optional<CryptoFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return forceGetPhysicalFolder().exists();
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		return forceGetPhysicalFolder().creationTime();
	}

	@Override
	public Instant lastModified() {
		return forceGetPhysicalFolder().lastModified();
	}

	@Override
	public void delete() {
		throw new UnsupportedOperationException("Can not delete CryptoFileSytem root.");
	}

	@Override
	public void moveTo(Folder target) {
		throw new UnsupportedOperationException("Can not move CryptoFileSytem root.");
	}

	@Override
	public void create() {
		forceGetPhysicalFolder().create();
	}

	@Override
	public String toString() {
		return "/";
	}

}
