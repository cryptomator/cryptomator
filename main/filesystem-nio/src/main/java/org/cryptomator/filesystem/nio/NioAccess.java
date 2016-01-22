package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import org.cryptomator.common.Holder;

interface NioAccess {

	public static final Holder<NioAccess> DEFAULT = new Holder<>(new DefaultNioAccess());

	AsynchronousFileChannel open(Path path, OpenOption... options) throws IOException;

	boolean isRegularFile(Path path, LinkOption... options);

	boolean exists(Path path, LinkOption... options);

	boolean isDirectory(Path childPath, LinkOption... options);

	Stream<Path> list(Path dir) throws IOException;

	void createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException;

	FileTime getLastModifiedTime(Path path, LinkOption... options) throws IOException;

	void delete(Path path) throws IOException;

	void close(AsynchronousFileChannel channel) throws IOException;

	void move(Path source, Path target, CopyOption... options) throws IOException;

	void setLastModifiedTime(Path path, FileTime time) throws IOException;

	String separator();

	FileTime getCreationTime(Path path, LinkOption... options) throws IOException;

	void setCreationTime(Path path, FileTime creationTime, LinkOption... options) throws IOException;

}
