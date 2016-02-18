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
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoEngineTestModule;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.crypto.CryptoFileSystemTestComponent;
import org.cryptomator.filesystem.crypto.DaggerCryptoFileSystemTestComponent;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.frontend.webdav.filters.LoggingHttpFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.mockito.Mockito;

public class InMemoryWebDavServer {

	private static final CryptoFileSystemTestComponent CRYPTO_FS_COMP = DaggerCryptoFileSystemTestComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build();
	private static final WebDavComponent WEVDAV_COMP = DaggerWebDavComponent.create();

	public static void main(String[] args) throws Exception {
		WebDavServer server = WEVDAV_COMP.server();
		server.setPort(8080);
		server.start();

		FileSystem fileSystem = cryptoFileSystem();
		ServletContextHandler servlet = server.addServlet(fileSystem, URI.create("http://localhost:8080/foo"));
		servlet.addFilter(LoggingHttpFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servlet.start();

		System.out.println("Server started. Press any key to stop it...");
		System.in.read();
		server.stop();
	}

	private static FileSystem cryptoFileSystem() {
		FileSystem shorteningFileSystem = shorteningFileSystem();
		CRYPTO_FS_COMP.cryptoFileSystemFactory().initializeNew(shorteningFileSystem, "asd");
		return CRYPTO_FS_COMP.cryptoFileSystemFactory().unlockExisting(shorteningFileSystem, "asd", Mockito.mock(CryptoFileSystemDelegate.class));
	}

	private static FileSystem shorteningFileSystem() {
		return CRYPTO_FS_COMP.shorteningFileSystemFactory().get(inMemoryFileSystem());
	}

	private static FileSystem inMemoryFileSystem() {
		return new InMemoryFileSystem();
	}

}
