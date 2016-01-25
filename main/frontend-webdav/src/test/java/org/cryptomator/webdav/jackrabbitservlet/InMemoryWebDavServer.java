/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.nio.ByteBuffer;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;

public class InMemoryWebDavServer {

	public static void main(String[] args) throws Exception {
		FileSystem fileSystem = setupFilesystem();
		FileSystemBasedWebDavServer server = new FileSystemBasedWebDavServer(fileSystem);

		server.start();
		System.out.println("Server started. Press any key to stop it...");
		System.in.read();
		server.stop();
	}

	private static FileSystem setupFilesystem() {
		FileSystem fileSystem = new InMemoryFileSystem();
		fileSystem.folder("mamals").folder("cats").create();
		fileSystem.folder("mamals").folder("dogs").create();
		try (WritableFile writable = fileSystem.folder("mamals").folder("cats").file("Garfield.txt").openWritable()) {
			writable.write(ByteBuffer.wrap("meow".getBytes()));
		}
		return fileSystem;
	}

}
