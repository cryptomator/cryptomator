package org.cryptomator.filesystem.invariants;

import static org.cryptomator.common.test.TempFilesRemovedOnShutdown.createTempDirectory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoEngineTestModule;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.crypto.CryptoFileSystemTestComponent;
import org.cryptomator.filesystem.crypto.DaggerCryptoFileSystemTestComponent;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.cryptomator.filesystem.shortening.ShorteningFileSystem;
import org.mockito.Mockito;

class FileSystemFactories implements Iterable<FileSystemFactory> {

	private static final CryptoFileSystemTestComponent CRYPTO_FS_COMP = DaggerCryptoFileSystemTestComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build();

	private final List<FileSystemFactory> factories = new ArrayList<>();

	public FileSystemFactories() {
		add("NioFileSystem", this::createNioFileSystem);
		add("InMemoryFileSystem", this::createInMemoryFileSystem);
		add("CryptoFileSystem(NioFileSystem)", this::createCryptoFileSystemNio);
		add("CryptoFileSystem(InMemoryFileSystem)", this::createCryptoFileSystemInMemory);
		add("ShorteningFileSystem(NioFileSystem)", this::createShorteningFileSystemNio);
		add("ShorteningFileSystem(InMemoryFileSystem)", this::createShorteningFileSystemInMemory);
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
		FileSystem delegate = createInMemoryFileSystem();
		CRYPTO_FS_COMP.cryptoFileSystemFactory().initializeNew(delegate, "aPassphrase");
		return CRYPTO_FS_COMP.cryptoFileSystemFactory().unlockExisting(delegate, "aPassphrase", Mockito.mock(CryptoFileSystemDelegate.class));
	}

	private FileSystem createCryptoFileSystemNio() {
		FileSystem delegate = createNioFileSystem();
		CRYPTO_FS_COMP.cryptoFileSystemFactory().initializeNew(delegate, "aPassphrase");
		return CRYPTO_FS_COMP.cryptoFileSystemFactory().unlockExisting(delegate, "aPassphrase", Mockito.mock(CryptoFileSystemDelegate.class));
	}

	private FileSystem createShorteningFileSystemNio() {
		FileSystem delegate = createNioFileSystem();
		return new ShorteningFileSystem(delegate.folder("d"), delegate.folder("m"), 3);
	}

	private FileSystem createShorteningFileSystemInMemory() {
		FileSystem delegate = createInMemoryFileSystem();
		return new ShorteningFileSystem(delegate.folder("d"), delegate.folder("m"), 3);
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
