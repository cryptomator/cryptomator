package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.function.Function;

class JWEHelper {

	private static final Logger LOG = LoggerFactory.getLogger(JWEHelper.class);
	private static final String JWE_PAYLOAD_KEY_FIELD = "key";
	private static final String EC_ALG = "EC";

	private JWEHelper(){}

	public static ECPrivateKey decryptUserKey(JWEObject jwe, ECPrivateKey deviceKey) {
		try {
			jwe.decrypt(new ECDHDecrypter(deviceKey));
			var keySpec = readKey(jwe, JWE_PAYLOAD_KEY_FIELD, PKCS8EncodedKeySpec::new);
			var factory = KeyFactory.getInstance(EC_ALG);
			var privateKey = factory.generatePrivate(keySpec);
			if (privateKey instanceof ECPrivateKey ecPrivateKey) {
				return ecPrivateKey;
			} else {
				throw new IllegalStateException(EC_ALG + " key factory not generating ECPrivateKeys");
			}
		} catch (JOSEException e) {
			LOG.warn("Failed to decrypt JWE: {}", jwe);
			throw new MasterkeyLoadingFailedException("Failed to decrypt JWE", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(EC_ALG + " not supported");
		} catch (InvalidKeySpecException e) {
			LOG.warn("Unexpected JWE payload: {}", jwe.getPayload());
			throw new MasterkeyLoadingFailedException("Unexpected JWE payload", e);
		}
	}

	public static Masterkey decryptVaultKey(JWEObject jwe, ECPrivateKey privateKey) throws MasterkeyLoadingFailedException {
		try {
			jwe.decrypt(new ECDHDecrypter(privateKey));
			return readKey(jwe, JWE_PAYLOAD_KEY_FIELD, Masterkey::new);
		} catch (JOSEException e) {
			LOG.warn("Failed to decrypt JWE: {}", jwe);
			throw new MasterkeyLoadingFailedException("Failed to decrypt JWE", e);
		}
	}

	private static <T> T readKey(JWEObject jwe, String keyField, Function<byte[], T> rawKeyFactory) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(jwe.getState() == JWEObject.State.DECRYPTED);
		var fields = jwe.getPayload().toJSONObject();
		if (fields == null) {
			LOG.error("Expected JWE payload to be JSON: {}", jwe.getPayload());
			throw new MasterkeyLoadingFailedException("Expected JWE payload to be JSON");
		}
		var keyBytes = new byte[0];
		try {
			if (fields.get(keyField) instanceof String key) {
				keyBytes = BaseEncoding.base64().decode(key);
				return rawKeyFactory.apply(keyBytes);
			} else {
				throw new IllegalArgumentException("JWE payload doesn't contain field " + keyField);
			}
		} catch (IllegalArgumentException e) {
			LOG.error("Unexpected JWE payload: {}", jwe.getPayload());
			throw new MasterkeyLoadingFailedException("Unexpected JWE payload", e);
		} finally {
			Arrays.fill(keyBytes, (byte) 0x00);
		}
	}
}
