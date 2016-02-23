package org.cryptomator.filesystem.delegating;

import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public interface DelegatingFileSystem extends FileSystem {

	Folder getDelegate();

	@Override
	default Optional<Long> quotaUsedBytes() {
		return getDelegate().fileSystem().quotaUsedBytes();
	}

	@Override
	default Optional<Long> quotaAvailableBytes() {
		return getDelegate().fileSystem().quotaAvailableBytes();
	}

}
