package org.cryptomator.filesystem.invariants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.invariants.WaysToObtainAFolder.WayToObtainAFolder;

class WaysToObtainAFolder implements Iterable<WayToObtainAFolder> {

	private final List<WayToObtainAFolder> values = new ArrayList<>();

	public WaysToObtainAFolder() {
		addNonExisting("invoke folder", this::invokeFolder);
		addNonExisting("create and delete", this::createAndDeleteFolder);
		addNonExisting("delete by moving", this::deleteFolderByMoving);

		addExisting("invoke folder and create", this::invokeFolderAndCreate);
		addExisting("create by moving", this::createByMoving);
		addExisting("create by copying", this::createByCopying);
	}

	private Folder invokeFolder(Folder parent, String name) {
		return parent.folder(name);
	}

	private Folder invokeFolderAndCreate(Folder parent, String name) {
		Folder result = parent.folder(name);
		result.create();
		return result;
	}

	private Folder createAndDeleteFolder(Folder parent, String name) {
		Folder result = parent.folder(name);
		result.create();
		result.delete();
		return result;
	}

	private Folder deleteFolderByMoving(Folder parent, String name) {
		Folder result = parent.folder(name);
		result.create();
		Folder target = parent.folder("willNotExistMoveFolderAway");
		result.moveTo(target);
		target.delete();
		return result;
	}

	private Folder createByMoving(Folder parent, String name) {
		Folder temporary = parent.folder("willNotExistCreateByMoving");
		temporary.create();
		Folder target = parent.folder(name);
		temporary.moveTo(target);
		return target;
	}

	private Folder createByCopying(Folder parent, String name) {
		Folder temporary = parent.folder("willNotExistCreateByCopying");
		temporary.create();
		Folder target = parent.folder(name);
		temporary.copyTo(target);
		temporary.delete();
		return target;
	}

	private void addExisting(String name, WayToObtainAFolderThatExists factory) {
		values.add(new WayToObtainAFolderThatExists() {
			@Override
			public Folder folderWithName(Folder parent, String name) {
				return factory.folderWithName(parent, name);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	private void addNonExisting(String name, WayToObtainAFolderThatDoesntExists factory) {
		values.add(new WayToObtainAFolderThatDoesntExists() {
			@Override
			public Folder folderWithName(Folder parent, String name) {
				return factory.folderWithName(parent, name);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	public interface WayToObtainAFolder {

		Folder folderWithName(Folder parent, String name);

		boolean returnedFoldersExist();

	}

	private interface WayToObtainAFolderThatExists extends WayToObtainAFolder {
		@Override
		default boolean returnedFoldersExist() {
			return true;
		}
	}

	private interface WayToObtainAFolderThatDoesntExists extends WayToObtainAFolder {
		@Override
		default boolean returnedFoldersExist() {
			return false;
		}
	}

	@Override
	public Iterator<WayToObtainAFolder> iterator() {
		return values.iterator();
	}

}
