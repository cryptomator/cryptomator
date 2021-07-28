package org.cryptomator.ui.keyloading.hub;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

class X509Helper {

	private static final X500Name ISSUER = new X500Name("CN=Cryptomator");
	private static final X500Name SUBJECT = new X500Name("CN=Self Signed Cert");
	private static final ASN1ObjectIdentifier ASN1_SUBJECT_KEY_ID = new ASN1ObjectIdentifier("2.5.29.14");

	private X509Helper() {}

	/**
	 * Creates a self-signed X509Certificate containing the public key and signed with the private key of a given key pair.
	 *
	 * @param keyPair A key pair
	 * @param signatureAlg A signature algorithm suited for the given key pair (see <a href="https://docs.oracle.com/en/java/javase/16/docs/specs/security/standard-names.html#signature-algorithms">available algorithms</a>)
	 * @return A self-signed X509Certificate
	 * @throws CertificateException If certificate generation failed, e.g. because of unsupported algorithms
	 */
	public static X509Certificate createSelfSignedCert(KeyPair keyPair, String signatureAlg) throws CertificateException {
		try {
			X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder( //
					ISSUER, //
					randomSerialNo(), //
					Date.from(Instant.now()), //
					Date.from(Instant.now().plus(3650, ChronoUnit.DAYS)), //
					SUBJECT, //
					keyPair.getPublic());
			certificateBuilder.addExtension(ASN1_SUBJECT_KEY_ID, false, getX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));
			var signer = new JcaContentSignerBuilder(signatureAlg).build(keyPair.getPrivate());
			var cert = certificateBuilder.build(signer);
			try (InputStream in = new ByteArrayInputStream(cert.getEncoded())) {
				return (X509Certificate) getCertFactory().generateCertificate(in);
			}
		} catch (IOException | OperatorCreationException e) {
			throw new CertificateException(e);
		}
	}

	private static BigInteger randomSerialNo() {
		return BigInteger.valueOf(UUID.randomUUID().getMostSignificantBits());
	}

	private static JcaX509ExtensionUtils getX509ExtensionUtils() {
		try {
			return new JcaX509ExtensionUtils();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support SHA-1.");
		}
	}

	private static CertificateFactory getCertFactory() {
		try {
			return CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support X.509.");
		}
	}

}
