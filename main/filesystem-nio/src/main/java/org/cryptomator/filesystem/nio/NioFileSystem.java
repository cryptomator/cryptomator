package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.nio.file.Files;
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
		try {
			long availableBytes = Files.getFileStore(path).getUsableSpace();
			long totalBytes = Files.getFileStore(path).getTotalSpace();
			return Optional.of(totalBytes - availableBytes);
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	@Override
	public Optional<Long> quotaAvailableBytes() {
		try {
			long availableBytes = Files.getFileStore(path).getUsableSpace();
			return Optional.of(availableBytes);
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

}
