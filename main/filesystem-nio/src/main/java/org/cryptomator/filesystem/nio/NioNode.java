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

	private NioFileSystem fileSystem;

	public NioNode(Optional<NioFolder> parent, Path path) {
		this.path = path.toAbsolutePath();
		this.parent = parent;
	}

	NioFileSystem fileSystem() {
		if (fileSystem == null) {
			fileSystem = parent //
					.map(NioNode::fileSystem) //
					.orElseGet(() -> (NioFileSystem) this);
		}
		return fileSystem;
	}

	boolean belongsToSameFilesystem(Node other) {
		return other instanceof NioNode //
				&& ((NioNode) other).fileSystem() == fileSystem();
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
