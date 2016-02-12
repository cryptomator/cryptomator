package org.cryptomator.filesystem.jackrabbit;

import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.LazyInitializer;

/**
 * Adds package-private API to {@link FileSystemResourceLocator}.
 */
interface InternalFileSystemResourceLocator extends FileSystemResourceLocator {

	@Override
	default String getResourcePath() {
		return LazyInitializer.initializeLazily(getResourcePathRef(), this::computeResourcePath);
	}

	AtomicReference<String> getResourcePathRef();

	String computeResourcePath();

}
