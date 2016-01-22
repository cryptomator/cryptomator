package org.cryptomator.filesystem.invariants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.invariants.WaysToObtainAFile.WayToObtainAFile;

class WaysToObtainAFile implements Iterable<WayToObtainAFile> {

	private final List<WayToObtainAFile> values = new ArrayList<>();

	public WaysToObtainAFile() {
		addNonExisting("invoke file", this::invokeFile);

		addExisting("create file by writing to it", this::createFileByWritingToIt);
	}

	private File invokeFile(Folder parent, String name, byte[] content) {
		return parent.file(name);
	}

	private File createFileByWritingToIt(Folder parent, String name, byte[] content) {
		File result = parent.file(name);
		try (WritableFile writable = result.openWritable()) {
			writable.write(ByteBuffer.wrap(content));
		}
		return result;
	}

	private void addExisting(String name, WayToObtainAFileThatExists factory) {
		values.add(new WayToObtainAFileThatExists() {
			@Override
			public File fileWithNameAndContent(Folder parent, String name, byte[] content) {
				return factory.fileWithNameAndContent(parent, name, content);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	private void addNonExisting(String name, WayToObtainAFileThatDoesntExist factory) {
		values.add(new WayToObtainAFileThatDoesntExist() {
			@Override
			public File fileWithNameAndContent(Folder parent, String name, byte[] content) {
				return factory.fileWithNameAndContent(parent, name, content);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	public interface WayToObtainAFile {

		default File fileWithName(Folder parent, String name) {
			return fileWithNameAndContent(parent, name, new byte[0]);
		}

		File fileWithNameAndContent(Folder parent, String name, byte[] content);

		boolean returnedFilesExist();

	}

	private interface WayToObtainAFileThatExists extends WayToObtainAFile {
		@Override
		default boolean returnedFilesExist() {
			return true;
		}
	}

	private interface WayToObtainAFileThatDoesntExist extends WayToObtainAFile {
		@Override
		default boolean returnedFilesExist() {
			return false;
		}
	}

	@Override
	public Iterator<WayToObtainAFile> iterator() {
		return values.iterator();
	}

}
