package org.cryptomator.filesystem.nio.integrationtests;

import static org.cryptomator.common.test.TempFilesRemovedOnShutdown.createTempDirectory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.apache.commons.io.IOUtils;

class FilesystemSetupUtils {

	public static Path emptyFilesystem() {
		try {
			return createTempDirectory("test-filesystem");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path testFilesystem(Entry firstEntry, Entry... entries) {
		try {
			Path root = createTempDirectory("test-filesystem");
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
		private Instant lastModified;

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

		public FileEntry withLastModified(Instant lastModified) {
			this.lastModified = lastModified;
			return this;
		}

		@Override
		public void create(Path root) throws IOException {
			Path filePath = root.resolve(relativePath);
			Files.createDirectories(filePath.getParent());
			try (OutputStream out = Files.newOutputStream(filePath)) {
				IOUtils.write(data, out);
			}
			if (lastModified != null) {
				Files.setLastModifiedTime(filePath, FileTime.from(lastModified));
			}
		}
	}

	public static class FolderEntry implements Entry {
		private Path relativePath;
		private Instant lastModified;

		public FolderEntry(Path relativePath) {
			this.relativePath = relativePath;
		}

		public FolderEntry withLastModified(Instant lastModified) {
			this.lastModified = lastModified;
			return this;
		}

		@Override
		public void create(Path root) throws IOException {
			Path path = root.resolve(relativePath);
			Files.createDirectories(path);
			if (lastModified != null) {
				Files.setLastModifiedTime(path, FileTime.from(lastModified));
			}
		}
	}

}
