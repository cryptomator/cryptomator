package org.cryptomator.common;

import com.google.common.io.BaseEncoding;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXParameters;
import java.util.ArrayList;
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

	static void validateChain(List<X509Certificate> certificateChain, X509Certificate rootCertificate) throws GeneralSecurityException {
		if (certificateChain.isEmpty()) {
			throw new IllegalArgumentException("Certificate chain must not be empty.");
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
		CertPathValidator.getInstance("PKIX").validate(certPath, params);
	}

	static Optional<String> extractCommonName(X509Certificate cert) {
		String rfc2253Name = cert.getSubjectX500Principal().getName(X500Principal.RFC2253);
		for (String rdn : splitRdns(rfc2253Name)) {
			int equalsPos = rdn.indexOf('=');
			if (equalsPos > 0 && "CN".equalsIgnoreCase(rdn.substring(0, equalsPos).trim())) {
				return Optional.of(unescapeRdnValue(rdn.substring(equalsPos + 1).trim()));
			}
		}
		return Optional.empty();
	}

	private static List<String> splitRdns(String distinguishedName) {
		List<String> rdns = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean escaped = false;
		for (int i = 0; i < distinguishedName.length(); i++) {
			char c = distinguishedName.charAt(i);
			if (escaped) {
				current.append(c);
				escaped = false;
			} else if (c == '\\') {
				current.append(c);
				escaped = true;
			} else if (c == ',') {
				rdns.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		rdns.add(current.toString());
		return rdns;
	}

	private static String unescapeRdnValue(String value) {
		StringBuilder unescaped = new StringBuilder(value.length());
		boolean escaped = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (escaped) {
				unescaped.append(c);
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else {
				unescaped.append(c);
			}
		}
		return unescaped.toString();
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
