package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.filesystem.FileSystemVisitor.fileSystemVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.WritableFile;

class NioFolder extends NioNode implements Folder {

	private final WeakValuedCache<Path, NioFolder> folders = WeakValuedCache.usingLoader(this::folderFromPath);
	private final WeakValuedCache<Path, NioFile> files = WeakValuedCache.usingLoader(this::fileFromPath);

	public NioFolder(Optional<NioFolder> parent, Path path) {
		super(parent, path);
	}

	@Override
	public Stream<? extends Node> children() throws UncheckedIOException {
		try {
			return Files.list(path).map(this::childPathToNode);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private NioNode childPathToNode(Path childPath) {
		if (Files.isDirectory(childPath)) {
			return folders.get(childPath);
		} else {
			return files.get(childPath);
		}
	}

	private NioFile fileFromPath(Path path) {
		return new NioFile(Optional.of(this), path);
	}

	private NioFolder folderFromPath(Path path) {
		return new NioFolder(Optional.of(this), path);
	}

	@Override
	public File file(String name) throws UncheckedIOException {
		return files.get(path.resolve(name));
	}

	@Override
	public Folder folder(String name) throws UncheckedIOException {
		return folders.get(path.resolve(name));
	}

	@Override
	public void create() throws UncheckedIOException {
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
			Files.move(path, target.path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String toString() {
		return format("NioFolder(%s)", path);
	}

	@Override
	public void delete() {
		fileSystemVisitor() //
				.forEachFile(NioFolder::deleteFile) //
				.afterFolder(NioFolder::deleteEmptyFolder) //
				.visit(this);
	}

	private static final void deleteFile(File file) {
		try (WritableFile writableFile = file.openWritable()) {
			writableFile.delete();
		}
	}

	private static final void deleteEmptyFolder(Folder folder) {
		try {
			Files.delete(((NioFolder) folder).path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
