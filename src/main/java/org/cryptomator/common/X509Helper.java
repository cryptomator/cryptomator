package org.cryptomator.common;

import com.google.common.io.BaseEncoding;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXParameters;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class X509Helper {

	private static final String CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
	private static final String CERT_END = "-----END CERTIFICATE-----";

	private X509Helper() {
	}

	static X509Certificate parsePemCertificate(String pemCertificate) {
		String base64 = pemCertificate.replace(CERT_BEGIN, "").replace(CERT_END, "").replaceAll("\\s", "");
		byte[] der = BaseEncoding.base64().decode(base64);
		return parseDerCertificate(der);
	}

	static List<X509Certificate> parseX5cCertificateChain(List<String> x5cEntries) {
		List<X509Certificate> certificates = new ArrayList<>(x5cEntries.size());
		for (String x5cEntry : x5cEntries) {
			byte[] der = BaseEncoding.base64().decode(x5cEntry);
			certificates.add(parseDerCertificate(der));
		}
		return certificates;
	}

	/**
	 * Validates the given leaf-first certificate chain against the trusted root.
	 *
	 * @param certificateChain The certificates, leaf first, as found in a JWS {@code x5c} header
	 * @param rootCertificate  The trust anchor the chain must lead to
	 * @param validAt          The point in time at which every certificate in the chain must be valid, i.e. the time the signature was made
	 * @throws GeneralSecurityException If the chain is not valid at the given time or does not lead to the trusted root
	 */
	static void validateChain(List<X509Certificate> certificateChain, X509Certificate rootCertificate, Instant validAt) throws GeneralSecurityException {
		if (certificateChain.isEmpty()) {
			throw new IllegalArgumentException("Certificate chain must not be empty.");
		}
		Date validationDate = Date.from(validAt);
		for (X509Certificate cert : certificateChain) {
			cert.checkValidity(validationDate);
		}

		List<X509Certificate> certPathCertificates = new ArrayList<>(certificateChain);
		if (rootCertificate.equals(certPathCertificates.get(certPathCertificates.size() - 1))) {
			certPathCertificates.remove(certPathCertificates.size() - 1);
		}
		if (certPathCertificates.isEmpty()) {
			throw new IllegalArgumentException("Certificate path must contain at least one non-root certificate.");
		}

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		CertPath certPath = certificateFactory.generateCertPath(certPathCertificates);
		PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(rootCertificate, null)));
		params.setRevocationEnabled(false);
		params.setDate(validationDate);
		CertPathValidator.getInstance("PKIX").validate(certPath, params);
	}

	static Optional<String> extractCommonName(X509Certificate cert) {
		try {
			var name = new LdapName(cert.getSubjectX500Principal().getName(X500Principal.RFC2253));
			for (Rdn rdn : name.getRdns().reversed()) { // LdapName lists RDNs right to left
				Attribute cn = rdn.toAttributes().get("CN");
				if (cn != null && cn.get() instanceof String value) {
					return Optional.of(value);
				}
			}
			return Optional.empty();
		} catch (NamingException e) {
			return Optional.empty();
		}
	}

	private static X509Certificate parseDerCertificate(byte[] derEncodedCertificate) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(derEncodedCertificate));
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("Invalid certificate.", e);
		}
	}
}
