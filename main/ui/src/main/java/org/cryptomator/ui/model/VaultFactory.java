package org.cryptomator.ui.model;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.filesystem.crypto.CryptoFileSystemFactory;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.frontend.webdav.mount.WebDavMounter;
import org.cryptomator.ui.util.DeferredCloser;

import dagger.Lazy;

@Singleton
public class VaultFactory {

	private final Lazy<FrontendFactory> frontendFactory;
	private final CryptoFileSystemFactory cryptoFileSystemFactory;
	private final DeferredCloser closer;

	@Inject
	public VaultFactory(Lazy<FrontendFactory> frontendFactory, CryptoFileSystemFactory cryptoFileSystemFactory, WebDavMounter mounter, DeferredCloser closer) {
		this.frontendFactory = frontendFactory;
		this.cryptoFileSystemFactory = cryptoFileSystemFactory;
		this.closer = closer;
	}

	public Vault createVault(Path path) {
		return new Vault(path, frontendFactory, cryptoFileSystemFactory, closer);
	}

}
