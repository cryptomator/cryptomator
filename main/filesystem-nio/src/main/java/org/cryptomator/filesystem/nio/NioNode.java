package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

abstract class NioNode implements Node {

	protected final Optional<NioFolder> parent;
	protected final Path path;

	protected final NioAccess nioAccess;
	protected final InstanceFactory instanceFactory;

	public NioNode(Optional<NioFolder> parent, Path path, NioAccess nioAccess, InstanceFactory instanceFactoy) {
		this.path = path.toAbsolutePath();
		this.parent = parent;
		this.nioAccess = nioAccess;
		this.instanceFactory = instanceFactoy;
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
	public Instant lastModified() throws UncheckedIOException {
		try {
			return nioAccess.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
