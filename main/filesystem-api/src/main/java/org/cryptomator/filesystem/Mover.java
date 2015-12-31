/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem;

class Mover {

	public static void move(File source, File destination) {
		if (source == destination) {
			return;
		}
		try (OpenFiles openFiles = DeadlockSafeFileOpener.withWritable(source).andWritable(destination).open()) {
			openFiles.writable(source).moveTo(openFiles.writable(destination));
		}
	}

}
