package org.cryptomator.shortening;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.Node;

class ShorteningFolder extends ShorteningNode<Folder> implements Folder {

	private final Folder metadataRoot;
	private final FilenameShortener shortener;

	public ShorteningFolder(ShorteningFolder parent, Folder delegate, String longName, Folder metadataRoot, FilenameShortener shortener) {
		super(parent, delegate, longName);
		this.metadataRoot = metadataRoot;
		this.shortener = shortener;
	}

	@Override
	public Stream<? extends Node> children() {
		return Stream.concat(this.files(), this.folders());
	}

	private ShorteningFile existingFile(File original) {
		final String longName = shortener.inflate(original.name());
		return new ShorteningFile(this, original, longName, shortener);
	}

	@Override
	public File file(String name) {
		final File original = delegate.file(shortener.deflate(name));
		if (metadataRoot.equals(original)) { // comparing apples and oranges,
												// but we don't know if the
												// underlying fs distinguishes
												// files and folders...
			throw new UncheckedIOException("'" + name + "' is a reserved name.", new FileAlreadyExistsException(name));
		}
		return new ShorteningFile(this, original, name, shortener);
	}

	@Override
	public Stream<? extends File> files() throws UncheckedIOException {
		return delegate.files().map(this::existingFile);
	}

	private ShorteningFolder existingFolder(Folder original) {
		final String longName = shortener.inflate(original.name());
		return new ShorteningFolder(this, original, longName, metadataRoot, shortener);
	}

	@Override
	public Folder folder(String name) {
		final Folder original = delegate.folder(shortener.deflate(name));
		if (metadataRoot.equals(original)) {
			throw new UncheckedIOException("'" + name + "' is a reserved name.", new FileAlreadyExistsException(name));
		}
		return new ShorteningFolder(this, original, name, metadataRoot, shortener);
	}

	@Override
	public Stream<? extends Folder> folders() {
		// if metadataRoot is inside our filesystem, we must filter it out:
		final Predicate<Node> equalsMetadataRoot = (Node node) -> metadataRoot.equals(node);
		return delegate.folders().filter(equalsMetadataRoot.negate()).map(this::existingFolder);
	}

	@Override
	public void create(FolderCreateMode mode) {
		if (!parent().get().exists() && FolderCreateMode.FAIL_IF_PARENT_IS_MISSING.equals(mode)) {
			throw new UncheckedIOException(new FileNotFoundException(parent().get().name()));
		} else if (!parent().get().exists() && FolderCreateMode.INCLUDING_PARENTS.equals(mode)) {
			parent().get().create(mode);
		}
		assert parent().get().exists();
		if (shortener.isShortened(shortName())) {
			shortener.saveMapping(name(), shortName());
		}
		delegate.create(mode);
	}

	@Override
	public void delete() {
		delegate.delete();
	}

	@Override
	public void moveTo(Folder target) {
		if (target instanceof ShorteningFolder) {
			moveToInternal((ShorteningFolder) target);
		} else {
			throw new UnsupportedOperationException("Can not move ShorteningFolder to conventional folder.");
		}
	}

	private void moveToInternal(ShorteningFolder target) {
		if (this.isAncestorOf(target) || target.isAncestorOf(this)) {
			throw new IllegalArgumentException("Can not move directories containing one another (src: " + this + ", dst: " + target + ")");
		}

		if (!target.exists()) {
			target.create(FolderCreateMode.INCLUDING_PARENTS);
		}

		delegate.moveTo(target.delegate);
	}

	@Override
	public String toString() {
		return parent + name() + "/";
	}

}
