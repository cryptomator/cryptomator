package org.cryptomator.filesystem.blacklisting;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

@Singleton
public class BlacklistingFileSystemFactory {

	@Inject
	public BlacklistingFileSystemFactory() {
	}

	public FileSystem get(Folder root, Predicate<Node> hiddenFiles) {
		return new BlacklistingFileSystem(root, hiddenFiles);
	}
}
