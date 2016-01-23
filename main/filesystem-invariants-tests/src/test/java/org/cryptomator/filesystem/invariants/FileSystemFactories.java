package org.cryptomator.filesystem.invariants;

import static java.nio.file.Files.createTempDirectory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.cryptomator.crypto.engine.impl.CryptorImpl;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoFileSystem;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.mockito.Mockito;

class FileSystemFactories implements Iterable<FileSystemFactory> {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {
		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}
	};

	private final List<FileSystemFactory> factories = new ArrayList<>();

	public FileSystemFactories() {
		add("NioFileSystem", this::createNioFileSystem);
		add("InMemoryFileSystem", this::createInMemoryFileSystem);
		add("CryptoFileSystem(NioFileSystem)", this::createCryptoFileSystemNio);
		add("CryptoFileSystem(InMemoryFileSystem)", this::createCryptoFileSystemInMemory);
	}

	private FileSystem createNioFileSystem() {
		try {
			return NioFileSystem.rootedAt(createTempDirectory("fileSystemToTest"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private FileSystem createInMemoryFileSystem() {
		return new InMemoryFileSystem();
	}

	private FileSystem createCryptoFileSystemInMemory() {
		return new CryptoFileSystem(createInMemoryFileSystem(), new CryptorImpl(RANDOM_MOCK), Mockito.mock(CryptoFileSystemDelegate.class), "aPassphrase");
	}

	private FileSystem createCryptoFileSystemNio() {
		return new CryptoFileSystem(createNioFileSystem(), new CryptorImpl(RANDOM_MOCK), Mockito.mock(CryptoFileSystemDelegate.class), "aPassphrase");
	}

	private void add(String name, FileSystemFactory factory) {
		factories.add(new FileSystemFactory() {
			@Override
			public FileSystem create() {
				return factory.create();
			}

			@Override
			public String toString() {
				return name;
			}
		});
	}

	@Override
	public Iterator<FileSystemFactory> iterator() {
		return factories.iterator();
	}

	public interface FileSystemFactory {

		FileSystem create();

	}

}
