package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

class DefaultNioAccess implements NioAccess {

	@Override
	public AsynchronousFileChannel open(Path path, OpenOption... options) throws IOException {
		return AsynchronousFileChannel.open(path, options);
	}

	@Override
	public boolean isRegularFile(Path path, LinkOption... options) {
		return Files.isRegularFile(path, options);
	}

	@Override
	public boolean exists(Path path, LinkOption... options) {
		return Files.exists(path, options);
	}

	@Override
	public boolean isDirectory(Path path, LinkOption... options) {
		return Files.isDirectory(path, options);
	}

	@Override
	public Stream<Path> list(Path dir) throws IOException {
		return Files.list(dir);
	}

	@Override
	public void createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
		Files.createDirectories(dir, attrs);
	}

	@Override
	public FileTime getLastModifiedTime(Path path, LinkOption... options) throws IOException {
		return Files.getLastModifiedTime(path, options);
	}

	@Override
	public void delete(Path path) throws IOException {
		Files.delete(path);
	}

	@Override
	public void close(AsynchronousFileChannel channel) throws IOException {
		channel.close();
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		Files.move(source, target, options);
	}

	@Override
	public void setLastModifiedTime(Path path, FileTime time) throws IOException {
		Files.setLastModifiedTime(path, time);
	}

	@Override
	public String separator() {
		return FileSystems.getDefault().getSeparator();
	}

	@Override
	public FileTime getCreationTime(Path path, LinkOption... options) throws IOException {
		return Files.readAttributes(path, BasicFileAttributes.class, options).creationTime();
	}

	@Override
	public void setCreationTime(Path path, FileTime creationTime, LinkOption... options) throws IOException {
		Files.getFileAttributeView(path, BasicFileAttributeView.class, options).setTimes(null, null, creationTime);
	}

}
