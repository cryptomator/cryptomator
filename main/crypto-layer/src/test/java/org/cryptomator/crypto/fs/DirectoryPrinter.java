/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import java.util.Optional;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;

public final class DirectoryPrinter {

	private DirectoryPrinter() {
	}

	public static String print(Folder folder) {
		StringBuilder sb = new StringBuilder(folder.name()).append('\n');

		DirectoryWalker.walk(folder, (node) -> {
			Optional<? extends Folder> parent = node.parent();
			while (parent.isPresent()) {
				sb.append("  ");
				parent = parent.get().parent();
			}
			if (node instanceof Folder) {
				sb.append(node.name()).append('/').append('\n');
			} else if (node instanceof File) {
				sb.append(node.name()).append('\n');
			}
		});

		return sb.toString();
	}

}
