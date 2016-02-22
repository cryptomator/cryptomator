package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.common.Holder;
import org.cryptomator.filesystem.FileSystem;

interface InstanceFactory {

	public static final Holder<InstanceFactory> DEFAULT = new Holder<>(new DefaultInstanceFactory());

	NioFolder nioFolder(Optional<NioFolder> parent, Path path, NioAccess nioAccess);

	NioFile nioFile(Optional<NioFolder> parent, Path path, NioAccess nioAccess);

	SharedFileChannel sharedFileChannel(Path path, NioAccess nioAccess);

	WritableNioFile writableNioFile(FileSystem fileSystem, Path path, SharedFileChannel channel, Runnable afterCloseCallback);

	ReadableNioFile readableNioFile(Path path, SharedFileChannel channel, Runnable afterCloseCallback);

}
