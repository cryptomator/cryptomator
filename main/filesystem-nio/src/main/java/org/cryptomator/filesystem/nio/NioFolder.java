package org.cryptomator.filesystem.nio;

import static org.cryptomator.filesystem.FolderCreateMode.INCLUDING_PARENTS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.Node;

class NioFolder extends NioNode implements Folder {

	private final WeakValuedCache<Path, NioFolder> folders = WeakValuedCache.usingLoader(this::folderFromPath);
	private final WeakValuedCache<Path, NioFile> files = WeakValuedCache.usingLoader(this::fileFromPath);

	public NioFolder(Optional<NioFolder> parent, Path path, NioNodeFactory nodeFactory) {
		super(parent, path, nodeFactory);
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
		return nodeFactory.file(Optional.of(this), path);
	}

	private NioFolder folderFromPath(Path path) {
		return nodeFactory.folder(Optional.of(this), path);
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
	public void create(FolderCreateMode mode) throws UncheckedIOException {
		NioFolderCreateMode.valueOf(mode).create(path);
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
			target.parent().ifPresent(folder -> folder.create(INCLUDING_PARENTS));
			Files.move(path, target.path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
