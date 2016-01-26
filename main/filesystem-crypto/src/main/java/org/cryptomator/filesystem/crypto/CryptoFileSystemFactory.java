package org.cryptomator.filesystem.crypto;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.blockaligned.BlockAlignedFileSystemFactory;
import org.cryptomator.filesystem.shortening.ShorteningFileSystemFactory;

@Singleton
public class CryptoFileSystemFactory {

	private final Provider<Cryptor> cryptorProvider;
	private final ShorteningFileSystemFactory shorteningFileSystemFactory;
	private final BlockAlignedFileSystemFactory blockAlignedFileSystemFactory;

	@Inject
	public CryptoFileSystemFactory(Provider<Cryptor> cryptorProvider, ShorteningFileSystemFactory shorteningFileSystemFactory, BlockAlignedFileSystemFactory blockAlignedFileSystemFactory) {
		this.cryptorProvider = cryptorProvider;
		this.shorteningFileSystemFactory = shorteningFileSystemFactory;
		this.blockAlignedFileSystemFactory = blockAlignedFileSystemFactory;
	}

	public FileSystem get(Folder root, CharSequence passphrase, CryptoFileSystemDelegate delegate) throws InvalidPassphraseException {
		final FileSystem nameShorteningFs = shorteningFileSystemFactory.get(root);
		final FileSystem cryptoFs = new CryptoFileSystem(nameShorteningFs, cryptorProvider.get(), delegate, passphrase);
		return blockAlignedFileSystemFactory.get(cryptoFs);
	}
}
