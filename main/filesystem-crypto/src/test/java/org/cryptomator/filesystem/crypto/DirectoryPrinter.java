/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.FileSystemVisitor.fileSystemVisitor;

import org.cryptomator.filesystem.Folder;

public final class DirectoryPrinter {

	private DirectoryPrinter() {
	}

	public static String print(Folder rootFolder) {
		StringBuilder result = new StringBuilder();
		StringBuilder indentation = new StringBuilder();
		fileSystemVisitor() //
				.beforeFolder(folder -> {
					result.append(indentation).append(folder.name()).append("/\n");
					indentation.append("  ");
				}) //
				.afterFolder(folder -> {
					indentation.delete(indentation.length() - 2, indentation.length());
				}).forEachFile(file -> {
					result.append(indentation).append(file.name()).append('\n');
				}) //
				.visit(rootFolder);
		return result.toString();
	}

}
