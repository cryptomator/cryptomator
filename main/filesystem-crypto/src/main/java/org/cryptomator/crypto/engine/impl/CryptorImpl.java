/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import org.cryptomator.common.LazyInitializer;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FilenameCryptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CryptorImpl implements Cryptor {

	private static final int SCRYPT_SALT_LENGTH = 8;
	private static final int SCRYPT_COST_PARAM = 1 << 14;
	private static final int SCRYPT_BLOCK_SIZE = 8;
	private static final int KEYLENGTH_IN_BYTES = 32;
	private static final String ENCRYPTION_ALG = "AES";
	private static final String MAC_ALG = "HmacSHA256";

	private SecretKey encryptionKey;
	private SecretKey macKey;
	private final AtomicReference<FilenameCryptor> filenameCryptor = new AtomicReference<>();
	private final AtomicReference<FileContentCryptor> fileContentCryptor = new AtomicReference<>();
	private final SecureRandom randomSource;

	CryptorImpl(SecureRandom randomSource) {
		this.randomSource = randomSource;
	}

	@Override
	public FilenameCryptor getFilenameCryptor() {
		assertKeysExist();
		return LazyInitializer.initializeLazily(filenameCryptor, () -> {
			return new FilenameCryptorImpl(encryptionKey, macKey);
		});
	}

	@Override
	public FileContentCryptor getFileContentCryptor() {
		assertKeysExist();
		return LazyInitializer.initializeLazily(fileContentCryptor, () -> {
			return new FileContentCryptorImpl(encryptionKey, macKey, randomSource);
		});
	}

	private void assertKeysExist() {
		if (encryptionKey == null || encryptionKey.isDestroyed()) {
			throw new IllegalStateException("No or invalid encryptionKey.");
		}
		if (macKey == null || macKey.isDestroyed()) {
			throw new IllegalStateException("No or invalid MAC key.");
		}
	}

	@Override
	public void randomizeMasterkey() {
		final byte[] randomBytes = new byte[KEYLENGTH_IN_BYTES];
		try {
			randomSource.nextBytes(randomBytes);
			encryptionKey = new SecretKeySpec(randomBytes, ENCRYPTION_ALG);
			randomSource.nextBytes(randomBytes);
			macKey = new SecretKeySpec(randomBytes, ENCRYPTION_ALG);
		} finally {
			Arrays.fill(randomBytes, (byte) 0x00);
		}
	}

	@Override
	public boolean readKeysFromMasterkeyFile(byte[] masterkeyFileContents, CharSequence passphrase) {
		final KeyFile keyFile;
		try {
			final ObjectMapper om = new ObjectMapper();
			keyFile = om.readValue(masterkeyFileContents, KeyFile.class);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to parse masterkeyFileContents", e);
		}

		// check version
		if (keyFile.getVersion() != KeyFile.CURRENT_VERSION) {
			// TODO
			// throw new UnsupportedVaultException(keyfile.getVersion(), KeyFile.CURRENT_VERSION);
			throw new IllegalArgumentException("Unsupported key (expected version: " + KeyFile.CURRENT_VERSION + ", actual version: " + keyFile.getVersion() + ")");
		}

		final byte[] kekBytes = Scrypt.scrypt(passphrase, keyFile.getScryptSalt(), keyFile.getScryptCostParam(), keyFile.getScryptBlockSize(), KEYLENGTH_IN_BYTES);
		try {
			final SecretKey kek = new SecretKeySpec(kekBytes, ENCRYPTION_ALG);
			this.encryptionKey = AesKeyWrap.unwrap(kek, keyFile.getEncryptionMasterKey(), ENCRYPTION_ALG);
			this.macKey = AesKeyWrap.unwrap(kek, keyFile.getMacMasterKey(), MAC_ALG);
			return true;
		} catch (InvalidKeyException e) {
			return false;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Hard-coded algorithm doesn't exist.", e);
		} finally {
			Arrays.fill(kekBytes, (byte) 0x00);
		}
	}

	@Override
	public byte[] writeKeysToMasterkeyFile(CharSequence passphrase) {
		final byte[] scryptSalt = new byte[SCRYPT_SALT_LENGTH];
		randomSource.nextBytes(scryptSalt);

		final byte[] kekBytes = Scrypt.scrypt(passphrase, scryptSalt, SCRYPT_COST_PARAM, SCRYPT_BLOCK_SIZE, KEYLENGTH_IN_BYTES);
		final byte[] wrappedEncryptionKey;
		final byte[] wrappedMacKey;
		try {
			final SecretKey kek = new SecretKeySpec(kekBytes, ENCRYPTION_ALG);
			wrappedEncryptionKey = AesKeyWrap.wrap(kek, encryptionKey);
			wrappedMacKey = AesKeyWrap.wrap(kek, macKey);
		} finally {
			Arrays.fill(kekBytes, (byte) 0x00);
		}

		final KeyFile keyfile = new KeyFile();
		keyfile.setVersion(KeyFile.CURRENT_VERSION);
		keyfile.setScryptSalt(scryptSalt);
		keyfile.setScryptCostParam(SCRYPT_COST_PARAM);
		keyfile.setScryptBlockSize(SCRYPT_BLOCK_SIZE);
		keyfile.setEncryptionMasterKey(wrappedEncryptionKey);
		keyfile.setMacMasterKey(wrappedMacKey);

		try {
			final ObjectMapper om = new ObjectMapper();
			return om.writeValueAsBytes(keyfile);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to create JSON from " + keyfile, e);
		}
	}

	/* ======================= destruction ======================= */

	@Override
	public void destroy() throws DestroyFailedException {
		destroyQuietly(encryptionKey);
		destroyQuietly(macKey);
	}

	@Override
	public boolean isDestroyed() {
		return encryptionKey.isDestroyed() && macKey.isDestroyed();
	}

	private void destroyQuietly(Destroyable d) {
		try {
			d.destroy();
		} catch (DestroyFailedException e) {
			// ignore
		}
	}

}
