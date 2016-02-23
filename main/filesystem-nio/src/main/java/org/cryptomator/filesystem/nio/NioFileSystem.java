package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;

public class NioFileSystem extends NioFolder implements FileSystem {

	public static NioFileSystem rootedAt(Path root) {
		return new NioFileSystem(root);
	}

	private NioFileSystem(Path root) {
		super(Optional.empty(), root, NioAccess.DEFAULT.get(), InstanceFactory.DEFAULT.get());
		create();
	}

	@Override
	public Optional<Long> quotaUsedBytes() {
		// TODO du -sh
		return Optional.empty();
	}

	@Override
	public Optional<Long> quotaAvailableBytes() {
		// TODO df -lh
		return Optional.empty();
	}

}
