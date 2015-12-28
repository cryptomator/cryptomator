/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class CryptoFolder extends CryptoNode implements Folder {

	static final String FILE_EXT = ".dir";

	private final AtomicReference<String> directoryId = new AtomicReference<>();

	public CryptoFolder(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	@Override
	protected String encryptedName() {
		return cryptor.getFilenameCryptor().encryptFilename(name()) + FILE_EXT;
	}

	Folder physicalFolder() {
		final String encryptedThenHashedDirId = cryptor.getFilenameCryptor().hashDirectoryId(getDirectoryId());
		return physicalDataRoot().folder(encryptedThenHashedDirId.substring(0, 2)).folder(encryptedThenHashedDirId.substring(2));
	}

	protected String getDirectoryId() {
		if (directoryId.get() == null) {
			File dirFile = physicalFile();
			if (dirFile.exists()) {
				try (ReadableFile readable = dirFile.openReadable()) {
					final ByteBuffer buf = ByteBuffer.allocate(64);
					readable.read(buf);
					buf.flip();
					byte[] bytes = new byte[buf.remaining()];
					buf.get(bytes);
					directoryId.set(new String(bytes));
				}
			} else {
				directoryId.compareAndSet(null, UUID.randomUUID().toString());
			}
		}
		return directoryId.get();
	}

	@Override
	public Instant lastModified() {
		return physicalFile().lastModified();
	}

	@Override
	public Stream<? extends Node> children() {
		return Stream.concat(files(), folders());
	}

	@Override
	public Stream<CryptoFile> files() {
		return physicalFolder().files().map(File::name).filter(s -> s.endsWith(CryptoFile.FILE_EXT)).map(this::decryptFileName).map(this::file);
	}

	private String decryptFileName(String encryptedFileName) {
		final String ciphertext = StringUtils.removeEnd(encryptedFileName, CryptoFile.FILE_EXT);
		return cryptor.getFilenameCryptor().decryptFilename(ciphertext);
	}

	@Override
	public CryptoFile file(String name) {
		return new CryptoFile(this, name, cryptor);
	}

	@Override
	public Stream<CryptoFolder> folders() {
		return physicalFolder().files().map(File::name).filter(s -> s.endsWith(CryptoFolder.FILE_EXT)).map(this::decryptFolderName).map(this::folder);
	}

	private String decryptFolderName(String encryptedFolderName) {
		final String ciphertext = StringUtils.removeEnd(encryptedFolderName, CryptoFolder.FILE_EXT);
		return cryptor.getFilenameCryptor().decryptFilename(ciphertext);
	}

	@Override
	public CryptoFolder folder(String name) {
		return new CryptoFolder(this, name, cryptor);
	}

	@Override
	public void create(FolderCreateMode mode) {
		final File dirFile = physicalFile();
		if (dirFile.exists()) {
			return;
		}
		if (!parent.exists() && FolderCreateMode.FAIL_IF_PARENT_IS_MISSING.equals(mode)) {
			throw new UncheckedIOException(new FileNotFoundException(parent.name));
		} else if (!parent.exists() && FolderCreateMode.INCLUDING_PARENTS.equals(mode)) {
			parent.create(mode);
		}
		assert parent.exists();
		final String directoryId = getDirectoryId();
		try (WritableFile writable = dirFile.openWritable()) {
			final ByteBuffer buf = ByteBuffer.wrap(directoryId.getBytes());
			writable.write(buf);
		}
		physicalFolder().create(FolderCreateMode.INCLUDING_PARENTS);
	}

	@Override
	public void moveTo(Folder target) {
		if (target instanceof CryptoFolder) {
			moveToInternal((CryptoFolder) target);
		} else {
			throw new UnsupportedOperationException("Can not move CryptoFolder to conventional folder.");
		}
	}

	private void moveToInternal(CryptoFolder target) {
		if (this.isAncestorOf(target) || target.isAncestorOf(this)) {
			throw new IllegalArgumentException("Can not move directories containing one another (src: " + this + ", dst: " + target + ")");
		}

		target.physicalFile().parent().get().create(FolderCreateMode.INCLUDING_PARENTS);
		assert target.physicalFile().parent().get().exists();
		this.physicalFile().moveTo(target.physicalFile());

		// directoryId is now used by target, we must no longer use the same id
		// (we'll generate a new one when needed)
		directoryId.set(null);
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		return parent.toString() + name + "/";
	}

}
