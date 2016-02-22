package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

	// visible for testing
	Path path() {
		return path;
	}

	@Override
	public String name() throws UncheckedIOException {
		return path.getFileName().toString();
	}

	@Override
	public Optional<? extends Folder> parent() throws UncheckedIOException {
		return parent;
	}

	private static final Instant JANNUARY_THE_SECOND_NINTEENHUNDRED_SEVENTY = Instant.parse("1970-01-02T00:00:00Z");

	@Override
	public Instant lastModified() throws UncheckedIOException {
		try {
			return nioAccess.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void setLastModified(Instant lastModified) throws UncheckedIOException {
		try {
			nioAccess.setLastModifiedTime(path, FileTime.from(lastModified));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		try {
			Instant instant = nioAccess.getCreationTime(path).toInstant();
			if (instant.isBefore(JANNUARY_THE_SECOND_NINTEENHUNDRED_SEVENTY)) {
				return Optional.empty();
			} else {
				return Optional.of(instant);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void setCreationTime(Instant creationTime) throws UncheckedIOException {
		try {
			nioAccess.setCreationTime(path, FileTime.from(creationTime));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
