package org.cryptomator.filesystem.inmem;

import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;

public class InMemoryFileSystem extends InMemoryFolder implements FileSystem {

	public InMemoryFileSystem() {
		super(null, "", Instant.now());
	}

	@Override
	public Optional<InMemoryFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public void delete() {
		// no-op.
	}

	@Override
	public String toString() {
		return "/";
	}

}
