package org.cryptomator.filesystem.invariants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.invariants.FileBiFunctions.FileBiFunction;

class FileBiFunctions implements Iterable<FileBiFunction> {

	private final List<FileBiFunction> factories = new ArrayList<>();

	public FileBiFunctions() {
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

	private void addExisting(String name, ExistingFileBiFunction factory) {
		factories.add(new ExistingFileBiFunction() {
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

	private void addNonExisting(String name, NonExistingFileBiFunction factory) {
		factories.add(new NonExistingFileBiFunction() {
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

	public interface FileBiFunction {

		File fileWithName(Folder parent, String name);

		boolean returnedFilesExist();

	}

	public interface ExistingFileBiFunction extends FileBiFunction {
		@Override
		default boolean returnedFilesExist() {
			return true;
		}
	}

	public interface NonExistingFileBiFunction extends FileBiFunction {
		@Override
		default boolean returnedFilesExist() {
			return false;
		}
	}

	@Override
	public Iterator<FileBiFunction> iterator() {
		return factories.iterator();
	}

}
