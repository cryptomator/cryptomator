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

		addExisting("create file by writing to it", this::createFileUsingTouch);
	}

	private File invokeFile(Folder parent, String name) {
		return parent.file(name);
	}

	private File createFileUsingTouch(Folder parent, String name) {
		File result = parent.file(name);
		try (WritableFile writable = result.openWritable()) {
			writable.write(ByteBuffer.wrap(new byte[] {1}));
		}
		return result;
	}

	private void addExisting(String name, WayToObtainAFileThatExists factory) {
		values.add(new WayToObtainAFileThatExists() {
			@Override
			public File fileWithName(Folder parent, String name) {
				return factory.fileWithName(parent, name);
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
			public File fileWithName(Folder parent, String name) {
				return factory.fileWithName(parent, name);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	public interface WayToObtainAFile {

		File fileWithName(Folder parent, String name);

		boolean returnedFilesExist();

	}

	public interface WayToObtainAFileThatExists extends WayToObtainAFile {
		@Override
		default boolean returnedFilesExist() {
			return true;
		}
	}

	public interface WayToObtainAFileThatDoesntExist extends WayToObtainAFile {
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
