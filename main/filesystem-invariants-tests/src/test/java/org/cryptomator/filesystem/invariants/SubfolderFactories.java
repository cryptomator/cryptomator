package org.cryptomator.filesystem.invariants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.invariants.SubfolderFactories.SubfolderFactory;

class SubfolderFactories implements Iterable<SubfolderFactory> {

	private final List<SubfolderFactory> factories = new ArrayList<>();

	public SubfolderFactories() {
		addNonExisting("invoke folder", this::invokeFolder);
		addNonExisting("create and delete", this::createAndDeleteFolder);
		addNonExisting("delete by moving", this::moveFolderAway);

		addExisting("invoke folder and create", this::invokeFolderAndCreate);
		addExisting("create by moving", this::createByMoving);
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

	private Folder moveFolderAway(Folder parent, String name) {
		Folder result = parent.folder(name);
		result.create();
		Folder target = parent.folder("subfolderFactoryMoveFolderAway");
		result.moveTo(target);
		target.delete();
		return result;
	}

	private Folder createByMoving(Folder parent, String name) {
		Folder temporary = parent.folder("subfolderFactoryCreateByMoving");
		temporary.create();
		Folder target = parent.folder(name);
		temporary.moveTo(target);
		return target;
	}

	private void addExisting(String name, ExistingSubfolderFactory factory) {
		factories.add(new ExistingSubfolderFactory() {
			@Override
			public Folder subfolderWithName(Folder parent, String name) {
				return factory.subfolderWithName(parent, name);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	private void addNonExisting(String name, NonExistingSubfolderFactory factory) {
		factories.add(new NonExistingSubfolderFactory() {
			@Override
			public Folder subfolderWithName(Folder parent, String name) {
				return factory.subfolderWithName(parent, name);
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	public interface SubfolderFactory {

		Folder subfolderWithName(Folder parent, String name);

		boolean createsExistingFolder();

	}

	public interface ExistingSubfolderFactory extends SubfolderFactory {
		@Override
		default boolean createsExistingFolder() {
			return true;
		}
	}

	public interface NonExistingSubfolderFactory extends SubfolderFactory {
		@Override
		default boolean createsExistingFolder() {
			return false;
		}
	}

	@Override
	public Iterator<SubfolderFactory> iterator() {
		return factories.iterator();
	}

}
