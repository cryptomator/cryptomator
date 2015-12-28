package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

class FilesystemSetupUtils {

	public static Path emptyFilesystem() {
		try {
			return Files.createTempDirectory("test-filesystem");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path testFilesystem(Entry firstEntry, Entry... entries) {
		try {
			Path root = Files.createTempDirectory("test-filesystem");
			firstEntry.create(root);
			for (Entry entry : entries) {
				entry.create(root);
			}
			return root;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static FileEntry file(String path) {
		return new FileEntry(Paths.get(path));
	}

	public static FolderEntry folder(String path) {
		return new FolderEntry(Paths.get(path));
	}

	interface Entry {

		void create(Path root) throws IOException;

	}

	public static class FileEntry implements Entry {
		private Path relativePath;
		private byte[] data = new byte[0];

		public FileEntry(Path relativePath) {
			this.relativePath = relativePath;
		}

		public FileEntry withData(byte[] data) {
			this.data = data;
			return this;
		}

		public FileEntry withData(String data) {
			return withData(data.getBytes());
		}

		@Override
		public void create(Path root) throws IOException {
			Path filePath = root.resolve(relativePath);
			Files.createDirectories(filePath.getParent());
			try (OutputStream out = Files.newOutputStream(filePath)) {
				IOUtils.write(data, out);
			}
		}
	}

	public static class FolderEntry implements Entry {
		private Path relativePath;

		public FolderEntry(Path relativePath) {
			this.relativePath = relativePath;
		}

		@Override
		public void create(Path root) throws IOException {
			Files.createDirectories(root.resolve(relativePath));
		}
	}

}
