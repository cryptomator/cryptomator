package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import org.cryptomator.common.CachingSupplier;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.FileSystemFeature;

public class NioFileSystem extends NioFolder implements FileSystem {

	private final Supplier<Boolean> supportsCreationTime = CachingSupplier.from(this::supportsCreationTime);

	public static NioFileSystem rootedAt(Path root) {
		return new NioFileSystem(root);
	}

	private NioFileSystem(Path root) {
		super(Optional.empty(), root, NioAccess.DEFAULT.get(), InstanceFactory.DEFAULT.get());
		create();
	}

	@Override
	public boolean supports(FileSystemFeature feature) {
		if (feature == FileSystemFeature.CREATION_TIME_FEATURE) {
			return supportsCreationTime.get();
		} else {
			return false;
		}
	}

	private boolean supportsCreationTime() {
		return nioAccess.supportsCreationTime(path);
	}

}
