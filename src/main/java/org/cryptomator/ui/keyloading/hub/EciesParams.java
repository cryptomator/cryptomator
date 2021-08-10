package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * ECIES parameters required to decrypt the masterkey:
 * <ul>
 *     <li><code>m</code> Encrypted Masterkey (base64url-encoded ciphertext)</li>
 *     <li><code>epk</code> Ephemeral Public Key (base64url-encoded SPKI format)</li>
 * </ul>
 *
 * No separate tag required, since we use GCM for encryption.
 */
record EciesParams(String m, String epk) {

	public byte[] getCiphertext() {
		return BaseEncoding.base64Url().decode(m());
	}

	public ECPublicKey getEphemeralPublicKey() {
		try {
			byte[] keyBytes = BaseEncoding.base64Url().decode(epk());
			PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
			if (key instanceof ECPublicKey k) {
				return k;
			} else {
				throw new IllegalArgumentException("Key not an EC public key.");
			}
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException("Invalid license public key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
