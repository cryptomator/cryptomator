package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;

class DefaultInstanceFactory implements InstanceFactory {

	@Override
	public NioFolder nioFolder(Optional<NioFolder> parent, Path path, NioAccess nioAccess) {
		return new NioFolder(parent, path, nioAccess, this);
	}

	@Override
	public NioFile nioFile(Optional<NioFolder> parent, Path path, NioAccess nioAccess) {
		return new NioFile(parent, path, nioAccess, this);
	}

	@Override
	public SharedFileChannel sharedFileChannel(Path path, NioAccess nioAccess) {
		return new SharedFileChannel(path, nioAccess);
	}

	@Override
	public WritableNioFile writableNioFile(FileSystem fileSystem, Path path, SharedFileChannel channel, Runnable afterCloseCallback) {
		return new WritableNioFile(fileSystem, path, channel, afterCloseCallback);
	}

	@Override
	public ReadableNioFile readableNioFile(Path path, SharedFileChannel channel, Runnable afterCloseCallback) {
		return new ReadableNioFile(path, channel, afterCloseCallback);
	}

}
