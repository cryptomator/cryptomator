/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.WritableFile;

public class CryptoFileSystem extends CryptoFolder implements FileSystem {

	private static final String DATA_ROOT_DIR = "d";
	private static final String METADATA_ROOT_DIR = "m";
	private static final String ROOT_DIR_FILE = "root";
	private static final String MASTERKEY_FILE = "masterkey.cryptomator";
	private static final String MASTERKEY_BACKUP_FILE = "masterkey.cryptomator.bkup";

	private final Folder physicalRoot;

	public CryptoFileSystem(Folder physicalRoot, Cryptor cryptor) {
		super(null, "", cryptor);
		this.physicalRoot = physicalRoot;
	}

	@Override
	File physicalFile() {
		return physicalDataRoot().file(ROOT_DIR_FILE);
	}

	@Override
	Folder physicalDataRoot() {
		return physicalRoot.folder(DATA_ROOT_DIR);
	}

	@Override
	Folder physicalMetadataRoot() {
		return physicalRoot.folder(METADATA_ROOT_DIR);
	}

	@Override
	public Optional<CryptoFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return physicalRoot.exists();
	}

	@Override
	public void delete() {
		// no-op.
	}

	@Override
	public void create(FolderCreateMode mode) {
		physicalDataRoot().create(mode);
		physicalMetadataRoot().create(mode);
		final File dirFile = physicalFile();
		final String directoryId = getDirectoryId();
		try (WritableFile writable = dirFile.openWritable(1, TimeUnit.SECONDS)) {
			final ByteBuffer buf = ByteBuffer.wrap(directoryId.getBytes());
			writable.write(buf);
		} catch (TimeoutException e) {
			throw new UncheckedIOException(new IOException("Failed to lock directory file in time." + dirFile, e));
		}
		physicalFolder().create(FolderCreateMode.INCLUDING_PARENTS);
	}

	@Override
	public String toString() {
		return physicalRoot + ":::/";
	}

}
