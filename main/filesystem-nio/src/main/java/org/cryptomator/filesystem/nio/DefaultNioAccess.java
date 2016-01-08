package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.nio.channels.FileChannel;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultNioAccess implements NioAccess {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultNioAccess.class);

	@Override
	public FileChannel open(Path path, OpenOption... options) throws IOException {
		return FileChannel.open(path, options);
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
	public void close(FileChannel channel) throws IOException {
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

	@Override
	public boolean supportsCreationTime(Path path) {
		try {
			Path file = Files.createTempFile(path, "creationTimeCheck", "tmp");
			long expected = 1184725140000L;
			long millisecondsInADay = 86400000L;
			FileTime fileTime = FileTime.fromMillis(expected);
			Files.getFileAttributeView(file, BasicFileAttributeView.class).setTimes(null, null, fileTime);
			long actual = Files.readAttributes(file, BasicFileAttributes.class).creationTime().toMillis();
			return Math.abs(expected - actual) <= millisecondsInADay;
		} catch (IOException e) {
			LOG.info("supportsCreationTime failed", e);
			return false;
		}
	}

}
