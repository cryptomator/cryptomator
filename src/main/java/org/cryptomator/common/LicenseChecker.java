package org.cryptomator.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.common.io.BaseEncoding;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

@Singleton
class LicenseChecker {

	private final JWTVerifier verifier;

	@Inject
	public LicenseChecker(@Named("licensePublicKey") String pemEncodedPublicKey) {
		Algorithm algorithm = Algorithm.ECDSA512(decodePublicKey(pemEncodedPublicKey), null);
		this.verifier = JWT.require(algorithm).build();
	}

	private static ECPublicKey decodePublicKey(String pemEncodedPublicKey) {
		try {
			byte[] keyBytes = BaseEncoding.base64().decode(pemEncodedPublicKey);
			PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
			if (key instanceof ECPublicKey k) {
				return k;
			} else {
				throw new IllegalStateException("Key not an EC public key.");
			}
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException("Invalid license public key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public Optional<DecodedJWT> check(String licenseKey) {
		try {
			return Optional.of(verifier.verify(licenseKey));
		} catch (JWTVerificationException exception) {
			return Optional.empty();
		}
	}

}
