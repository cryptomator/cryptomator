package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

class NioNode implements Node {

	protected final Optional<NioFolder> parent;
	protected final Path path;
	protected final NioNodeFactory nodeFactory;

	public NioNode(Optional<NioFolder> parent, Path path, NioNodeFactory nodeFactory) {
		this.path = path.toAbsolutePath();
		this.nodeFactory = nodeFactory;
		this.parent = parent;
	}

	boolean belongsToSameFilesystem(Node other) {
		return other instanceof NioNode //
				&& ((NioNode) other).nodeFactory == nodeFactory;
	}

	@Override
	public String name() throws UncheckedIOException {
		return path.getFileName().toString();
	}

	@Override
	public Optional<? extends Folder> parent() throws UncheckedIOException {
		return parent;
	}

	@Override
	public boolean exists() throws UncheckedIOException {
		return Files.exists(path);
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
