/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import static org.cryptomator.filesystem.FileSystemVisitor.fileSystemVisitor;

import org.cryptomator.filesystem.Folder;

public final class DirectoryPrinter {

	private DirectoryPrinter() {
	}

	public static String print(Folder rootFolder) {
		StringBuilder result = new StringBuilder();
		StringBuilder identation = new StringBuilder();
		fileSystemVisitor() //
				.beforeFolder(folder -> {
					result.append(identation).append(folder.name()).append("/\n");
					identation.append("  ");
				}) //
				.afterFolder(folder -> {
					identation.delete(identation.length() - 2, identation.length());
				}).forEachFile(file -> {
					result.append(identation).append(file.name()).append('\n');
				}) //
				.visit(rootFolder);
		return result.toString();
	}

}
