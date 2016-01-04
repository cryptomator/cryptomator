package org.cryptomator.crypto;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.impl.FileContentCryptorImpl;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.blockaligned.BlockAlignedFileSystem;
import org.cryptomator.filesystem.crypto.CryptoFileSystem;

@Singleton
public class CryptoFileSystemFactory {

	private final Provider<Cryptor> cryptorProvider;

	@Inject
	public CryptoFileSystemFactory(Provider<Cryptor> cryptorProvider) {
		this.cryptorProvider = cryptorProvider;
	}

	public FileSystem get(Folder root, CharSequence passphrase) {
		return new BlockAlignedFileSystem(new CryptoFileSystem(root, cryptorProvider.get(), passphrase), FileContentCryptorImpl.CHUNK_SIZE);
	}
}
