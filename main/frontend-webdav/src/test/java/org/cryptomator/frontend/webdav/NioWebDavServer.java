/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Optional;

import javax.servlet.DispatcherType;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.cryptomator.frontend.webdav.filters.LoggingHttpFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class NioWebDavServer {

	private static final String PATH_TO_SERVE_PROPERTY = "pathToServe";
	private static final WebDavComponent WEVDAV_COMP = DaggerWebDavComponent.create();

	public static void main(String[] args) throws Exception {
		WebDavServer server = WEVDAV_COMP.server();
		server.setPort(8080);
		server.start();

		FileSystem fileSystem = setupFilesystem();
		ServletContextHandler servlet = server.addServlet(fileSystem, URI.create("http://localhost:8080/"));
		servlet.addFilter(LoggingHttpFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servlet.start();

		System.out.println("Server started. Press any key to stop it...");
		System.in.read();
		server.stop();
	}

	private static FileSystem setupFilesystem() {
		return NioFileSystem.rootedAt(pathToServe());
	}

	private static Path pathToServe() {
		Path path = pathFromSystemProperty().orElseGet(NioWebDavServer::pathEnteredByUser);
		if (!Files.isDirectory(path)) {
			throw new RuntimeException("Path is not a directory");
		}
		System.out.println("Serving " + path);
		return path;
	}

	private static Optional<Path> pathFromSystemProperty() {
		String value = System.getProperty(PATH_TO_SERVE_PROPERTY);
		if (value == null) {
			return Optional.empty();
		} else {
			return Optional.of(Paths.get(value));
		}
	}

	private static Path pathEnteredByUser() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
		System.out.print("Enter absolute path to serve (must be an existing directory): ");
		try {
			return Paths.get(in.readLine());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
