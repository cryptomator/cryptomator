package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;

class P12AccessHelper {

	private static final String EC_ALG = "EC";
	private static final String EC_CURVE_NAME = "secp256r1";
	private static final String SIGNATURE_ALG = "SHA256withECDSA";
	private static final String KEYSTORE_ALIAS_KEY = "key";
	private static final String KEYSTORE_ALIAS_CERT = "crt";

	private P12AccessHelper() {}

	/**
	 * Creates a new key pair and stores it in PKCS#12 format at the given path.
	 *
	 * @param p12File The path of the .p12 file
	 * @param pw The password to protect the key material
	 * @throws IOException In case of I/O errors
	 * @throws MasterkeyLoadingFailedException If any cryptographic operation fails
	 */
	public static KeyPair createNew(Path p12File, char[] pw) throws IOException, MasterkeyLoadingFailedException {
		try {
			var keyPair = getKeyPairGenerator().generateKeyPair();
			var keyStore = getKeyStore();
			keyStore.load(null, pw);
			var cert = X509Helper.createSelfSignedCert(keyPair, SIGNATURE_ALG);
			var chain = new X509Certificate[]{cert};
			keyStore.setKeyEntry(KEYSTORE_ALIAS_KEY, keyPair.getPrivate(), pw, chain);
			keyStore.setCertificateEntry(KEYSTORE_ALIAS_CERT, cert);
			var tmpFile = p12File.resolveSibling(p12File.getFileName().toString() + ".tmp");
			try (var out = Files.newOutputStream(tmpFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
				keyStore.store(out, pw);
			}
			Files.move(tmpFile, p12File, StandardCopyOption.REPLACE_EXISTING);
			return keyPair;
		} catch (GeneralSecurityException e) {
			throw new MasterkeyLoadingFailedException("Failed to store PKCS12 file.", e);
		}
	}

	/**
	 * Loads a key pair from a PKCS#12 file located at the given path.
	 *
	 * @param p12File The path of the .p12 file
	 * @param pw The password to protect the key material
	 * @throws IOException In case of I/O errors
	 * @throws InvalidPassphraseException If the supplied password is incorrect
	 * @throws MasterkeyLoadingFailedException If any cryptographic operation fails
	 */
	public static KeyPair loadExisting(Path p12File, char[] pw) throws IOException, InvalidPassphraseException, MasterkeyLoadingFailedException {
		try (var in = Files.newInputStream(p12File, StandardOpenOption.READ)) {
			var keyStore = getKeyStore();
			keyStore.load(in, pw);
			var sk = (PrivateKey) keyStore.getKey(KEYSTORE_ALIAS_KEY, pw);
			var pk = keyStore.getCertificate(KEYSTORE_ALIAS_CERT).getPublicKey();
			return new KeyPair(pk, sk);
		} catch (UnrecoverableKeyException e) {
			throw new InvalidPassphraseException();
		} catch (IOException e) {
			if (e.getCause() instanceof UnrecoverableKeyException) {
				throw new InvalidPassphraseException();
			} else {
				throw e;
			}
		} catch (GeneralSecurityException e) {
			throw new MasterkeyLoadingFailedException("Failed to load PKCS12 file.", e);
		}
	}

	private static KeyPairGenerator getKeyPairGenerator() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance(EC_ALG);
			keyGen.initialize(new ECGenParameterSpec(EC_CURVE_NAME));
			return keyGen;
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new IllegalStateException(EC_CURVE_NAME + " curve not supported");
		}
	}

	private static KeyStore getKeyStore() {
		try {
			return KeyStore.getInstance("PKCS12");
		} catch (KeyStoreException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support PKCS12.");
		}
	}

}
