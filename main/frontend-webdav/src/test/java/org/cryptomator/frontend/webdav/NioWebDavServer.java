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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoEngineTestModule;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.crypto.CryptoFileSystemTestComponent;
import org.cryptomator.filesystem.crypto.DaggerCryptoFileSystemTestComponent;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.mockito.Mockito;

public class NioWebDavServer {

	private static final CryptoFileSystemTestComponent CRYPTO_FS_COMP = DaggerCryptoFileSystemTestComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build();
	private static final String PATH_TO_SERVE_PROPERTY = "pathToServe";

	public static void main(String[] args) throws Exception {
		new FileSystemWebDavServer(cryptoFileSystem()).run();
	}

	private static FileSystem cryptoFileSystem() {
		FileSystem shorteningFileSystem = shorteningFileSystem();
		CRYPTO_FS_COMP.cryptoFileSystemFactory().initializeNew(shorteningFileSystem, "asd");
		return CRYPTO_FS_COMP.cryptoFileSystemFactory().unlockExisting(shorteningFileSystem, "asd", Mockito.mock(CryptoFileSystemDelegate.class));
	}

	private static FileSystem shorteningFileSystem() {
		return CRYPTO_FS_COMP.shorteningFileSystemFactory().get(nioFileSystem());
	}

	private static FileSystem nioFileSystem() {
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
