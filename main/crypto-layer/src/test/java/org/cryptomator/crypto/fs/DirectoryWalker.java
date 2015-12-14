/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

final class DirectoryWalker {

	private DirectoryWalker() {
	}

	public static void walk(Folder folder, Consumer<Node> visitor) {
		walk(folder, 0, Integer.MAX_VALUE, visitor);
	}

	public static void walk(Folder folder, int depth, int maxDepth, Consumer<Node> visitor) {
		try {
			folder.files().forEach(visitor);
			if (depth == maxDepth) {
				return;
			} else {
				folder.folders().forEach(childFolder -> {
					visitor.accept(childFolder);
					walk(childFolder, depth + 1, maxDepth, visitor);
				});
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
