package org.cryptomator.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.common.io.BaseEncoding;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Optional;

@Singleton
class LicenseChecker {

	private static final String LICENSE_ROOT_CERTIFICATE = """
			-----BEGIN CERTIFICATE-----
			MIIBqDCCAVqgAwIBAgIUKNImuR2JD+NyAWaYzb8V8w8SdFwwBQYDK2VwMD8xCzAJ
			BgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9MaWNl
			bnNlIFJvb3QgQ0EwIBcNMjYwMjI2MTMzMTQxWhgPMjA3NjAyMTQxMzMxNDFaMD8x
			CzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9M
			aWNlbnNlIFJvb3QgQ0EwKjAFBgMrZXADIQCOyUIv+3Ust66VWJ8yH5ruJGxyZC1u
			LK2Yxb+ZtPGAQKNmMGQwEgYDVR0TAQH/BAgwBgEB/wIBATAOBgNVHQ8BAf8EBAMC
			AQYwHQYDVR0OBBYEFGhKUh0jCta2wXzxrUldIBqB4Bz3MB8GA1UdIwQYMBaAFGhK
			Uh0jCta2wXzxrUldIBqB4Bz3MAUGAytlcANBAOERjFKpGnjxH1nh2u5lsCjX65zz
			XisC7XFaZQikVLKzHK+YTIusi3x7dGCFBjO/m3ieQpt7BsaPo0lLL719pQ8=
			-----END CERTIFICATE-----
			""";

	private final ECPublicKey legacyPublicKey;
	private final X509Certificate rootCertificate;
	private final String requiredChainCn;

	@Inject
	public LicenseChecker(@Named("licensePublicKey") String legacyLicensePublicKey, Environment environment) {
		this(legacyLicensePublicKey, LICENSE_ROOT_CERTIFICATE, environment.getLicenseChainRequiredCn());
	}

	@VisibleForTesting
	LicenseChecker(String legacyLicensePublicKey, String trustedRootCert, String requiredChainCn) {
		this.legacyPublicKey = decodePublicKey(legacyLicensePublicKey);
		this.rootCertificate = X509Helper.parsePemCertificate(trustedRootCert);
		this.requiredChainCn = requiredChainCn;
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
			DecodedJWT decodedJwt = JWT.decode(licenseKey);
			Claim x5cClaim = decodedJwt.getHeaderClaim("x5c");
			ECPublicKey signingKey;
			if (x5cClaim == null || x5cClaim.isMissing()) {
				signingKey = this.legacyPublicKey;
			} else {
				var certChain = verifyChain(x5cClaim);
				signingKey = asEcPublicKey(certChain.getFirst().getPublicKey());
			}
			JWTVerifier verifier = JWT.require(Algorithm.ECDSA512(signingKey, null)).build();
			return Optional.of(verifier.verify(licenseKey));
		} catch (JWTVerificationException | GeneralSecurityException e) {
			return Optional.empty();
		}
	}

	private List<X509Certificate> verifyChain(Claim x5cClaim) throws GeneralSecurityException {
		List<String> x5cEntries = x5cClaim.asList(String.class);
		if (x5cEntries == null || x5cEntries.isEmpty()) {
			throw new CertPathValidatorException("x5c claim is empty.");
		}
		List<X509Certificate> certChain = X509Helper.parseX5cCertificateChain(x5cEntries);
		boolean containsRequiredCn = certChain.stream() //
				.flatMap(cert -> X509Helper.extractCommonName(cert).stream()) //
				.anyMatch(requiredChainCn::equals);
		if (!containsRequiredCn) {
			throw new CertPathValidatorException("x5c certificate chain does not contain required CN " + requiredChainCn);
		}
		X509Helper.validateChain(certChain, rootCertificate);
		return certChain;
	}

	private static ECPublicKey asEcPublicKey(PublicKey publicKey) {
		if (publicKey instanceof ECPublicKey ecPublicKey) {
			return ecPublicKey;
		} else {
			throw new IllegalArgumentException("Leaf certificate key is not an EC public key.");
		}
	}

}
