package org.cryptomator.ui.model;

import java.nio.file.Path;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.ui.util.mount.WebDavMounter;

import com.google.inject.Inject;

public class VaultFactory {

	private final Cryptor cryptor;
	private final WebDavMounter mounter;

	@Inject
	public VaultFactory(Cryptor cryptor, WebDavMounter mounter) {
		this.cryptor = cryptor;
		this.mounter = mounter;
	}

	public Vault createVault(Path path) {
		return new Vault(path, cryptor, mounter);
	}

}
