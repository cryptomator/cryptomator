package org.cryptomator.filesystem.nio;

import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.cryptomator.common.WeakValuedCache;
import org.cryptomator.common.streams.AutoClosingStream;
import org.cryptomator.filesystem.Deleter;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

class NioFolder extends NioNode implements Folder {

	private final WeakValuedCache<Path, NioFolder> folders = WeakValuedCache.usingLoader(this::folderFromPath);
	private final WeakValuedCache<Path, NioFile> files = WeakValuedCache.usingLoader(this::fileFromPath);

	public NioFolder(Optional<NioFolder> parent, Path path, NioAccess nioAccess, InstanceFactory instanceFactory) {
		super(parent, path, nioAccess, instanceFactory);
	}

	@Override
	public Stream<? extends Node> children() throws UncheckedIOException {
		try {
			return AutoClosingStream.from(nioAccess.list(path).map(this::childPathToNode));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private NioNode childPathToNode(Path childPath) {
		if (nioAccess.isDirectory(childPath)) {
			return folders.get(childPath);
		} else {
			return files.get(childPath);
		}
	}

	private NioFile fileFromPath(Path path) {
		return instanceFactory.nioFile(Optional.of(this), path, nioAccess);
	}

	private NioFolder folderFromPath(Path path) {
		return instanceFactory.nioFolder(Optional.of(this), path, nioAccess);
	}

	@Override
	public File file(String name) throws UncheckedIOException {
		assertDoesNotContainsSeparator(name);
		return files.get(path.resolve(name));
	}

	@Override
	public Folder folder(String name) throws UncheckedIOException {
		assertDoesNotContainsSeparator(name);
		return folders.get(path.resolve(name));
	}

	private void assertDoesNotContainsSeparator(String name) {
		if (name.contains(nioAccess.separator())) {
			throw new IllegalArgumentException(format("Name must not contain file system separator (name: %s, separator: %s)", name, nioAccess.separator()));
		}
	}

	@Override
	public void create() throws UncheckedIOException {
		try {
			nioAccess.createDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		if (nioAccess.exists(path) && !nioAccess.isDirectory(path)) {
			throw new UncheckedIOException(new IOException(format("%s is a file", path)));
		}
		return super.lastModified();
	}

	@Override
	public boolean exists() throws UncheckedIOException {
		return nioAccess.isDirectory(path);
	}

	@Override
	public void moveTo(Folder target) {
		if (belongsToSameFilesystem(target)) {
			internalMoveTo((NioFolder) target);
		} else {
			throw new IllegalArgumentException("Can only move a Folder to a Folder in the same FileSystem");
		}
	}

	private void internalMoveTo(NioFolder target) {
		try {
			target.delete();
			target.parent().ifPresent(folder -> folder.create());
			nioAccess.move(path(), target.path());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	Path path() {
		return path;
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		if (nioAccess.exists(path) && !nioAccess.isDirectory(path)) {
			throw new UncheckedIOException(new IOException(format("%s is a file", path)));
		}
		return super.creationTime();
	}

	@Override
	public String toString() {
		return format("NioFolder(%s)", path);
	}

	@Override
	public void delete() {
		if (!exists()) {
			return;
		}
		Deleter.deleteContent(this);
		try {
			nioAccess.delete(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
