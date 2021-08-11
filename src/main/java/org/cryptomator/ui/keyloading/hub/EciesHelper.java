package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.CipherSupplier;
import org.cryptomator.cryptolib.common.DestroyableSecretKey;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

class EciesHelper {

	private static final int GCM_KEY_SIZE = 32;
	private static final int GCM_TAG_SIZE = 16;
	private static final int GCM_NONCE_SIZE = 12; // 96 bit IVs strongly recommended for GCM

	private EciesHelper() {}

	public static Masterkey decryptMasterkey(KeyPair deviceKey, EciesParams eciesParams) throws MasterkeyLoadingFailedException {
		var sharedSecret = ecdhAndKdf(deviceKey.getPrivate(), eciesParams.getEphemeralPublicKey(), GCM_KEY_SIZE + GCM_NONCE_SIZE);
		var cleartext = new byte[0];
		try (var kek = new DestroyableSecretKey(sharedSecret, 0, GCM_KEY_SIZE, "AES")) {
			var nonce = Arrays.copyOfRange(sharedSecret, GCM_KEY_SIZE, GCM_KEY_SIZE + GCM_NONCE_SIZE);
			var cipher = CipherSupplier.AES_GCM.forDecryption(kek, new GCMParameterSpec(GCM_TAG_SIZE * Byte.SIZE, nonce));
			cleartext = cipher.doFinal(eciesParams.getCiphertext());
			return new Masterkey(cleartext);
		} catch (AEADBadTagException e) {
			throw new MasterkeyLoadingFailedException("Unsuitable KEK to decrypt encrypted masterkey", e);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Unexpected exception during GCM decryption.", e);
		} finally {
			Arrays.fill(sharedSecret, (byte) 0x00);
			Arrays.fill(cleartext, (byte) 0x00);
		}
	}

	/**
	 * Computes a shared secret using ECDH key agreement and derives a key.
	 *
	 * @param privateKey Recipient's EC private key
	 * @param publicKey Sender's EC public key
	 * @param numBytes Number of bytes requested form KDF
	 * @return A derived secret key
	 */
	// visible for testing
	static byte[] ecdhAndKdf(PrivateKey privateKey, PublicKey publicKey, int numBytes) {
		Preconditions.checkArgument(privateKey instanceof ECPrivateKey, "expected ECPrivateKey");
		Preconditions.checkArgument(publicKey instanceof ECPublicKey, "expected ECPublicKey");
		byte[] sharedSecret = new byte[0];
		try {
			var keyAgreement = createKeyAgreement();
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(publicKey, true);
			sharedSecret = keyAgreement.generateSecret();
			return kdf(sharedSecret, new byte[0], numBytes);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException("Invalid keys", e);
		} finally {
			Arrays.fill(sharedSecret, (byte) 0x00);
		}
	}

	/**
	 * Performs <a href="https://www.secg.org/sec1-v2.pdf">ANSI-X9.63-KDF</a> with SHA-256
	 * @param sharedSecret A shared secret
	 * @param sharedInfo Additional authenticated data
	 * @param keyDataLen Desired key length (in bytes)
	 * @return key data
	 */
	// visible for testing
	static byte[] kdf(byte[] sharedSecret, byte[] sharedInfo, int keyDataLen) {
		MessageDigest digest = sha256(); // max input length is 2^64 - 1, see https://doi.org/10.6028/NIST.SP.800-56Cr2, Table 1
		int hashLen = digest.getDigestLength();

		// These two checks must be performed according to spec. However with 32 bit integers, we can't exceed any limits anyway:
		assert BigInteger.valueOf(sharedSecret.length + sharedInfo.length + 4).compareTo(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)) < 0: "input larger than hashmaxlen";
		assert keyDataLen < (2L << 32 - 1) * hashLen : "keyDataLen larger than hashLen × (2^32 − 1)";

		ByteBuffer counter = ByteBuffer.allocate(Integer.BYTES);
		assert ByteOrder.BIG_ENDIAN.equals(counter.order());
		int n = (keyDataLen + hashLen - 1) / hashLen;
		byte[] buffer = new byte[n * hashLen];
		try {
			for (int i = 0; i < n; i++) {
				digest.update(sharedSecret);
				counter.clear();
				counter.putInt(i + 1);
				counter.flip();
				digest.update(counter);
				digest.update(sharedInfo);
				digest.digest(buffer, i * hashLen, hashLen);
			}
			return Arrays.copyOf(buffer, keyDataLen);
		} catch (DigestException e) {
			throw new IllegalStateException("Invalid digest output buffer offset", e);
		} finally {
			Arrays.fill(buffer, (byte) 0x00);
		}
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support SHA-256.");
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
