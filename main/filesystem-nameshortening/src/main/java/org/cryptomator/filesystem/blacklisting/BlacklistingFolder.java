package org.cryptomator.filesystem.blacklisting;

import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.delegating.DelegatingFolder;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

class BlacklistingFolder extends DelegatingFolder<DelegatingReadableFile, DelegatingWritableFile, BlacklistingFolder, BlacklistingFile> {

	private final Predicate<Node> hiddenNodes;

	public BlacklistingFolder(BlacklistingFolder parent, Folder delegate, Predicate<Node> hiddenNodes) {
		super(parent, delegate);
		this.hiddenNodes = hiddenNodes;
	}

	@Override
	public Stream<? extends Node> children() {
		return Stream.concat(folders(), files());
	}

	@Override
	public Stream<BlacklistingFolder> folders() {
		return delegate.folders().filter(hiddenNodes.negate()).map(this::folder);
	}

	@Override
	public Stream<BlacklistingFile> files() {
		return delegate.files().filter(hiddenNodes.negate()).map(this::file);
	}

	@Override
	protected BlacklistingFile file(File delegate) {
		if (hiddenNodes.test(delegate)) {
			throw new UncheckedIOException("'" + delegate.name() + "' is a reserved name.", new FileAlreadyExistsException(delegate.name()));
		}
		return new BlacklistingFile(this, delegate);
	}

	@Override
	protected BlacklistingFolder folder(Folder delegate) {
		if (hiddenNodes.test(delegate)) {
			throw new UncheckedIOException("'" + delegate.name() + "' is a reserved name.", new FileAlreadyExistsException(delegate.name()));
		}
		return new BlacklistingFolder(this, delegate, hiddenNodes);
	}

}
