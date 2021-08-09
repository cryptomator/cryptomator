package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.AesKeyWrap;
import org.cryptomator.cryptolib.common.DestroyableSecretKey;

import javax.crypto.KeyAgreement;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

class EciesHelper {

	private EciesHelper() {}

	public static Masterkey decryptMasterkey(KeyPair deviceKey, EciesParams eciesParams) throws MasterkeyLoadingFailedException {
		// TODO: include a KDF between key agreement and KEK to conform to ECIES?
		try (var kek = ecdh(deviceKey.getPrivate(), eciesParams.getEphemeralPublicKey()); //
			 var rawMasterkey = AesKeyWrap.unwrap(kek, eciesParams.getCiphertext(), "HMAC")) {
			return new Masterkey(rawMasterkey.getEncoded());
		} catch (InvalidKeyException e) {
			throw new MasterkeyLoadingFailedException("Unsuitable KEK to decrypt encrypted masterkey", e);
		}
	}

	private static DestroyableSecretKey ecdh(PrivateKey privateKey, PublicKey publicKey) {
		Preconditions.checkArgument(privateKey instanceof ECPrivateKey, "expected ECPrivateKey");
		Preconditions.checkArgument(publicKey instanceof ECPublicKey, "expected ECPublicKey");
		byte[] keyBytes = new byte[0];
		try {
			var keyAgreement = createKeyAgreement();
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(publicKey, true);
			keyBytes = keyAgreement.generateSecret();
			return new DestroyableSecretKey(keyBytes, "AES");
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException("Invalid keys", e);
		} finally {
			Arrays.fill(keyBytes, (byte) 0x00);
		}
	}

	private static KeyAgreement createKeyAgreement() {
		try {
			return KeyAgreement.getInstance("ECDH");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("ECDH not supported");
		}
	}

}
