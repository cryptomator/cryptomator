package org.cryptomator.ui.model;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.mount.WebDavMounter;
import org.cryptomator.webdav.WebDavServer;

@Singleton
public class VaultFactory {

	private final WebDavServer server;
	private final Provider<Cryptor> cryptorProvider;
	private final WebDavMounter mounter;
	private final DeferredCloser closer;

	@Inject
	public VaultFactory(WebDavServer server, @Named("SamplingCryptor") Provider<Cryptor> cryptorProvider, WebDavMounter mounter, DeferredCloser closer) {
		this.server = server;
		this.cryptorProvider = cryptorProvider;
		this.mounter = mounter;
		this.closer = closer;
	}

	public Vault createVault(Path path) {
		return new Vault(path, server, cryptorProvider.get(), mounter, closer);
	}

}
