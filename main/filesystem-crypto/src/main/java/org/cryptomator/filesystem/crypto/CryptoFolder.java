/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
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
import java.io.Writer;
import java.nio.channels.Channels;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.common.WeakValuedCache;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.Deleter;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

class CryptoFolder extends CryptoNode implements Folder {

	static final String DIR_SUFFIX = "_";

	private final WeakValuedCache<String, CryptoFolder> folders = WeakValuedCache.usingLoader(this::newFolder);
	private final WeakValuedCache<String, CryptoFile> files = WeakValuedCache.usingLoader(this::newFile);
	private final AtomicReference<String> directoryId = new AtomicReference<>();

	public CryptoFolder(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	@Override
	protected String encryptedName() {
		final byte[] parentDirId = parent().map(CryptoFolder::getDirectoryId).map(s -> s.getBytes(UTF_8)).orElse(null);
		return cryptor.getFilenameCryptor().encryptFilename(name(), parentDirId) + DIR_SUFFIX;
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
		return physicalFolder().files().map(File::name).filter(isEncryptedFileName()).map(this::decryptChildFileName).map(this::file);
	}

	private Predicate<String> isEncryptedFileName() {
		return (String name) -> cryptor.getFilenameCryptor().isEncryptedFilename(name);
	}

	private String decryptChildFileName(String encryptedFileName) {
		final byte[] dirId = getDirectoryId().getBytes(UTF_8);
		return cryptor.getFilenameCryptor().decryptFilename(encryptedFileName, dirId);
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
		return physicalFolder().files().map(File::name).filter(isEncryptedDirectoryName()).map(this::decryptChildFolderName).map(this::folder);
	}

	private Predicate<String> isEncryptedDirectoryName() {
		return (String name) -> name.endsWith(DIR_SUFFIX) && isEncryptedFileName().test(StringUtils.removeEnd(name, DIR_SUFFIX));
	}

	private String decryptChildFolderName(String encryptedFolderName) {
		final byte[] dirId = getDirectoryId().getBytes(UTF_8);
		final String ciphertext = StringUtils.removeEnd(encryptedFolderName, CryptoFolder.DIR_SUFFIX);
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
		final Folder dir = physicalFolder();
		if (dirFile.exists() && dir.exists()) {
			return;
		}
		parent.create();
		final String directoryId = getDirectoryId();
		try (Writer writer = Channels.newWriter(dirFile.openWritable(), UTF_8.newEncoder(), -1)) {
			writer.write(directoryId);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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

		// directoryId will be used by target, from now on we must no longer use the same id
		// a new one will be generated on-demand.
		final String oldDirectoryId = getDirectoryId();
		directoryId.set(null);

		target.physicalFile().parent().get().create();
		this.physicalFile().moveTo(target.physicalFile());

		target.directoryId.set(oldDirectoryId);
	}

	@Override
	public void delete() {
		if (!exists()) {
			return;
		}
		Deleter.deleteContent(this);
		physicalFolder().delete();
		physicalFile().delete();
		directoryId.set(null);
	}

	@Override
	public String toString() {
		return parent.toString() + name + "/";
	}

}
