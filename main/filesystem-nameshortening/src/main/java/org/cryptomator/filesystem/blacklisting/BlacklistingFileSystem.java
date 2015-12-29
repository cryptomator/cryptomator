package org.cryptomator.filesystem.blacklisting;

import java.util.function.Predicate;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

public class BlacklistingFileSystem extends BlacklistingFolder implements FileSystem {

	public BlacklistingFileSystem(Folder root, Predicate<Node> hiddenNodes) {
		super(null, root, hiddenNodes);
	}

}
