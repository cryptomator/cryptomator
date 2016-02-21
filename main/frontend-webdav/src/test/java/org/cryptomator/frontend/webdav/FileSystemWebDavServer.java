/*******************************************************************************
 * Copyright (c) 2016 Markus Kreusch and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Markus Kreusch - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.frontend.webdav.filters.LoggingHttpFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;

class FileSystemWebDavServer {

	private static final WebDavComponent WEVDAV_COMP = DaggerWebDavComponent.create();

	private final FileSystem fileSystem;

	public FileSystemWebDavServer(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public void run() {
		try {
			tryRun();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void tryRun() throws Exception {
		WebDavServer server = WEVDAV_COMP.server();
		server.setPort(8080);
		server.start();

		ServletContextHandler servlet = server.addServlet(fileSystem, URI.create("http://localhost:8080/foo"));
		servlet.addFilter(LoggingHttpFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servlet.start();

		System.out.println("Server started. Press any key to stop it...");
		System.in.read();
		server.stop();
	}

}
