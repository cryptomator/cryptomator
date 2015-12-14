package org.cryptomator.crypto;

import java.io.IOException;
import java.util.Optional;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;

public class CryptoFileSystem extends CryptoFolder implements FileSystem {

	private static final String DATA_ROOT_DIR = "d";
	private static final String METADATA_ROOT_DIR = "m";
	private static final String ROOT_DIR_FILE = "root";
	private static final String MASTERKEY_FILE = "masterkey.cryptomator";
	private static final String MASTERKEY_BACKUP_FILE = "masterkey.cryptomator.bkup";

	private final Folder physicalRoot;

	public CryptoFileSystem(Folder physicalRoot) {
		super(null, "");
		this.physicalRoot = physicalRoot;
	}

	@Override
	File physicalFile() throws IOException {
		return physicalDataRoot().file(ROOT_DIR_FILE);
	}

	@Override
	Folder physicalFolder() throws IOException {
		// TODO Auto-generated method stub
		return super.physicalFolder();
	}

	@Override
	Folder physicalDataRoot() {
		return physicalRoot.folder(DATA_ROOT_DIR);
	}

	@Override
	Folder physicalMetadataRoot() {
		return physicalRoot.folder(METADATA_ROOT_DIR);
	}

	@Override
	public Optional<CryptoFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return physicalRoot.exists();
	}

	@Override
	public void delete() {
		// no-op.
	}

	@Override
	public String toString() {
		return "/";
	}

	@Override
	public void create(FolderCreateMode mode) throws IOException {
		physicalDataRoot().create(mode);
		physicalMetadataRoot().create(mode);
		super.create(mode);
	}

}
