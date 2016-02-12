/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.frontend.webdav.filters.LoggingHttpFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class InMemoryWebDavServer {

	public static void main(String[] args) throws Exception {
		WebDavServer server = DaggerWebDavComponent.create().server();
		server.setPort(8080);
		server.start();

		FileSystem fileSystem = setupFilesystem();
		ServletContextHandler servlet = server.addServlet(fileSystem, URI.create("http://localhost:8080/foo"));
		servlet.addFilter(LoggingHttpFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servlet.start();

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
