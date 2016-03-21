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
import static org.cryptomator.filesystem.crypto.Constants.DIR_SUFFIX;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.common.LazyInitializer;
import org.cryptomator.common.WeakValuedCache;
import org.cryptomator.common.streams.AutoClosingStream;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.Deleter;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.io.FileContents;

class CryptoFolder extends CryptoNode implements Folder {

	private final WeakValuedCache<String, CryptoFolder> folders = WeakValuedCache.usingLoader(this::newFolder);
	private final WeakValuedCache<String, CryptoFile> files = WeakValuedCache.usingLoader(this::newFile);
	private final AtomicReference<String> directoryId = new AtomicReference<>();

	public CryptoFolder(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	@Override
	protected Optional<String> encryptedName() {
		if (parent().isPresent()) {
			return parent().get().getDirectoryId().map(s -> s.getBytes(UTF_8)).map(parentDirId -> {
				return cryptor.getFilenameCryptor().encryptFilename(name(), parentDirId) + DIR_SUFFIX;
			});
		} else {
			return Optional.of(cryptor.getFilenameCryptor().encryptFilename(name()) + DIR_SUFFIX);
		}
	}

	Optional<Folder> physicalFolder() {
		if (getDirectoryId().isPresent()) {
			final String encryptedThenHashedDirId = cryptor.getFilenameCryptor().hashDirectoryId(getDirectoryId().get());
			return Optional.of(physicalDataRoot().folder(encryptedThenHashedDirId.substring(0, 2)).folder(encryptedThenHashedDirId.substring(2)));
		} else {
			return Optional.empty();
		}
	}

	Folder forceGetPhysicalFolder() {
		return physicalFolder().orElseThrow(() -> {
			return new UncheckedIOException(new FileNotFoundException(toString()));
		});
	}

	protected Optional<String> getDirectoryId() {
		return Optional.ofNullable(LazyInitializer.initializeLazily(directoryId, () -> {
			return physicalFile().filter(File::exists).map(FileContents.UTF_8::readContents).orElse(null);
		}));
	}

	@Override
	public Stream<? extends Node> children() {
		return AutoClosingStream.from(Stream.concat(files(), folders()));
	}

	@Override
	public Stream<CryptoFile> files() {
		final Stream<? extends File> files = physicalFolder().filter(Folder::exists).map(Folder::files).orElse(Stream.empty());
		return files.map(File::name).filter(isEncryptedFileName()).map(this::decryptChildFileName).map(this::file);
	}

	private Predicate<String> isEncryptedFileName() {
		return (String name) -> !name.endsWith(DIR_SUFFIX) && cryptor.getFilenameCryptor().isEncryptedFilename(name);
	}

	private String decryptChildFileName(String encryptedFileName) {
		final byte[] dirId = getDirectoryId().get().getBytes(UTF_8);
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
		final Stream<? extends File> files = physicalFolder().filter(Folder::exists).map(Folder::files).orElse(Stream.empty());
		return files.map(File::name).filter(isEncryptedDirectoryName()).map(this::decryptChildFolderName).map(this::folder);
	}

	private Predicate<String> isEncryptedDirectoryName() {
		return (String name) -> name.endsWith(DIR_SUFFIX) && cryptor.getFilenameCryptor().isEncryptedFilename(StringUtils.removeEnd(name, DIR_SUFFIX));
	}

	private String decryptChildFolderName(String encryptedFolderName) {
		final byte[] dirId = getDirectoryId().get().getBytes(UTF_8);
		final String ciphertext = StringUtils.removeEnd(encryptedFolderName, DIR_SUFFIX);
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
		parent.create();
		final boolean newDirIdGiven = directoryId.compareAndSet(null, UUID.randomUUID().toString());
		final File dirFile = forceGetPhysicalFile();
		final Folder dir = forceGetPhysicalFolder();
		if (dirFile.exists() && dir.exists()) {
			return;
		} else if (!newDirIdGiven) {
			throw new IllegalStateException("Newly created folder, that didn't exist before, already had an directoryId.");
		}
		if (parent.file(name).exists()) {
			throw new UncheckedIOException(new FileAlreadyExistsException(parent.file(name).toString()));
		}
		FileContents.UTF_8.writeContents(dirFile, directoryId.get());
		dir.create();
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
		assert target.parent().isPresent() : "Target can not be root, thus has a parent";

		// prepare target:
		target.delete();
		target.parent().get().create();

		// perform the actual move:
		final File dirFile = forceGetPhysicalFile();
		final String dirId = getDirectoryId().get();
		boolean dirIdMovedSuccessfully = target.directoryId.compareAndSet(null, dirId);
		if (!dirIdMovedSuccessfully) {
			throw new IllegalStateException("Target's directoryId wasn't null, even though it has been explicitly deleted.");
		}
		dirFile.moveTo(target.forceGetPhysicalFile());

		// cut all ties:
		this.invalidateDirectoryIdsRecursively();

		assert!exists();
		assert target.exists();
	}

	@Override
	public void delete() {
		if (!exists()) {
			assert directoryId.get() == null : "nonexisting folder still has a directory id";
			return;
		}
		Deleter.deleteContent(this);
		Folder physicalFolder = forceGetPhysicalFolder();
		physicalFolder.delete();
		Folder physicalFolderParent = physicalFolder.parent().get();
		if (physicalFolderParent.exists() && physicalFolderParent.folders().count() == 0) {
			physicalFolderParent.delete();
		}
		forceGetPhysicalFile().delete();
		invalidateDirectoryIdsRecursively();
	}

	private void invalidateDirectoryIdsRecursively() {
		directoryId.set(null);
		folders.forEach((name, folder) -> {
			folder.invalidateDirectoryIdsRecursively();
		});
	}

	@Override
	public String toString() {
		return parent.toString() + name + "/";
	}

}
