package org.cryptomator.crypto.engine.impl;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.FilenameCryptor;

public class CryptorImpl implements Cryptor {

	private final SecretKey encryptionKey;
	private final SecretKey macKey;
	private final FilenameCryptor filenameCryptor;

	public CryptorImpl(SecretKey encryptionKey, SecretKey macKey) {
		this.encryptionKey = encryptionKey;
		this.macKey = macKey;
		this.filenameCryptor = new FilenameCryptorImpl(encryptionKey, macKey);
	}

	@Override
	public FilenameCryptor getFilenameCryptor() {
		return filenameCryptor;
	}

	/* ======================= destruction ======================= */

	@Override
	public void destroy() throws DestroyFailedException {
		TheDestroyer.destroyQuietly(encryptionKey);
		TheDestroyer.destroyQuietly(macKey);
		TheDestroyer.destroyQuietly(filenameCryptor);
	}

	@Override
	public boolean isDestroyed() {
		return encryptionKey.isDestroyed() && macKey.isDestroyed() && filenameCryptor.isDestroyed();
	}

}
