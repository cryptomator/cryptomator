/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.common.WeakValuedCache;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.WritableFile;

class CryptoFolder extends CryptoNode implements Folder {

	static final String DIR_EXT = ".dir";

	private final WeakValuedCache<String, CryptoFolder> folders = WeakValuedCache.usingLoader(this::newFolder);
	private final WeakValuedCache<String, CryptoFile> files = WeakValuedCache.usingLoader(this::newFile);
	private final AtomicReference<String> directoryId = new AtomicReference<>();

	public CryptoFolder(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	@Override
	protected String encryptedName() {
		final byte[] parentDirId = parent().map(CryptoFolder::getDirectoryId).map(s -> s.getBytes(UTF_8)).orElse(null);
		return cryptor.getFilenameCryptor().encryptFilename(name(), parentDirId) + DIR_EXT;
	}

	Folder physicalFolder() {
		final String encryptedThenHashedDirId = cryptor.getFilenameCryptor().hashDirectoryId(getDirectoryId());
		return physicalDataRoot().folder(encryptedThenHashedDirId.substring(0, 2)).folder(encryptedThenHashedDirId.substring(2));
	}

	protected String getDirectoryId() {
		if (directoryId.get() == null) {
			File dirFile = physicalFile();
			if (dirFile.exists()) {
				try (Reader reader = Channels.newReader(dirFile.openReadable(), UTF_8.newDecoder(), -1)) {
					directoryId.set(IOUtils.toString(reader));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
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
		return physicalFolder().files().map(File::name).filter(s -> s.endsWith(CryptoFile.FILE_EXT)).map(this::decryptChildFileName).map(this::file);
	}

	private String decryptChildFileName(String encryptedFileName) {
		final byte[] dirId = getDirectoryId().getBytes(UTF_8);
		final String ciphertext = StringUtils.removeEnd(encryptedFileName, CryptoFile.FILE_EXT);
		return cryptor.getFilenameCryptor().decryptFilename(ciphertext, dirId);
	}

	@Override
	public CryptoFile file(String name) {
		return files.get(name);
	}

	public CryptoFile newFile(String name) {
		return new CryptoFile(this, name, cryptor);
	}

	@Override
	public Stream<CryptoFolder> folders() {
		return physicalFolder().files().map(File::name).filter(s -> s.endsWith(CryptoFolder.DIR_EXT)).map(this::decryptChildFolderName).map(this::folder);
	}

	private String decryptChildFolderName(String encryptedFolderName) {
		final byte[] dirId = getDirectoryId().getBytes(UTF_8);
		final String ciphertext = StringUtils.removeEnd(encryptedFolderName, CryptoFolder.DIR_EXT);
		return cryptor.getFilenameCryptor().decryptFilename(ciphertext, dirId);
	}

	@Override
	public CryptoFolder folder(String name) {
		return folders.get(name);
	}

	public CryptoFolder newFolder(String name) {
		return new CryptoFolder(this, name, cryptor);
	}

	@Override
	public void create() {
		final File dirFile = physicalFile();
		if (dirFile.exists()) {
			return;
		}
		parent.create();
		final String directoryId = getDirectoryId();
		try (WritableFile writable = dirFile.openWritable()) {
			final ByteBuffer buf = ByteBuffer.wrap(directoryId.getBytes());
			writable.write(buf);
		}
		physicalFolder().create();
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

		target.physicalFile().parent().get().create();
		this.physicalFile().moveTo(target.physicalFile());

		// directoryId is now used by target, we must no longer use the same id
		// (we'll generate a new one when needed)
		target.directoryId.set(getDirectoryId());
		directoryId.set(null);
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		return physicalFile().creationTime();
	}

	@Override
	public String toString() {
		return parent.toString() + name + "/";
	}

}
