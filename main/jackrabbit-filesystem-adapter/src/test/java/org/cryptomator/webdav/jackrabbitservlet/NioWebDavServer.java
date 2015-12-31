/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.nio.NioFileSystem;

public class NioWebDavServer {

	public static void main(String[] args) throws Exception {
		FileSystem fileSystem = setupFilesystem();
		FileSystemBasedWebDavServer server = new FileSystemBasedWebDavServer(fileSystem);

		server.start();
		System.out.println("Server started. Press any key to stop it...");
		System.in.read();
		server.stop();
	}

	private static FileSystem setupFilesystem() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
		System.out.print("Enter absolute path to serve (must be an existing directory): ");
		Path path = Paths.get(in.readLine());
		if (!Files.isDirectory(path)) {
			throw new RuntimeException("Path is not a directory");
		}
		return NioFileSystem.rootedAt(path);
	}

}
