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

import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;

class JWEHelper {

	private static final Logger LOG = LoggerFactory.getLogger(JWEHelper.class);
	private static final String JWE_PAYLOAD_MASTERKEY_FIELD = "key";

	private JWEHelper(){}

	public static Masterkey decrypt(JWEObject jwe, ECPrivateKey privateKey) throws MasterkeyLoadingFailedException {
		try {
			jwe.decrypt(new ECDHDecrypter(privateKey));
			return readKey(jwe);
		} catch (JOSEException e) {
			LOG.warn("Failed to decrypt JWE: {}", jwe);
			throw new MasterkeyLoadingFailedException("Failed to decrypt JWE", e);
		}
	}

	private static Masterkey readKey(JWEObject jwe) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(jwe.getState() == JWEObject.State.DECRYPTED);
		var fields = jwe.getPayload().toJSONObject();
		if (fields == null) {
			LOG.error("Expected JWE payload to be JSON: {}", jwe.getPayload());
			throw new MasterkeyLoadingFailedException("Expected JWE payload to be JSON");
		}
		var keyBytes = new byte[0];
		try {
			if (fields.get(JWE_PAYLOAD_MASTERKEY_FIELD) instanceof String key) {
				keyBytes = BaseEncoding.base64().decode(key);
				return new Masterkey(keyBytes);
			} else {
				throw new IllegalArgumentException("JWE payload doesn't contain field " + JWE_PAYLOAD_MASTERKEY_FIELD);
			}
		} catch (IllegalArgumentException e) {
			LOG.error("Unexpected JWE payload: {}", jwe.getPayload());
			throw new MasterkeyLoadingFailedException("Unexpected JWE payload", e);
		} finally {
			Arrays.fill(keyBytes, (byte) 0x00);
		}
	}
}
