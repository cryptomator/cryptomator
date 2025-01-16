package org.cryptomator.networking;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * SSLContextProvider for Linux using a PKCS#12 file as trust store
 */
@OperatingSystem(OperatingSystem.Value.LINUX)
@CheckAvailability
public class SSLContextWithPKCS12File extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	private static final String CERT_FILE_LOCATION_PROPERTY = "cryptomator.networking.truststore.p12Path";

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		var pkcs12FilePath = Path.of(System.getProperty(CERT_FILE_LOCATION_PROPERTY));
		try {
			return KeyStore.getInstance(pkcs12FilePath.toFile(), new char[]{});
		} catch (IllegalArgumentException e) {
			throw new NoSuchFileException(pkcs12FilePath.toString());
		}
	}

	@CheckAvailability
	public static boolean isSupported() {
		return System.getProperty(CERT_FILE_LOCATION_PROPERTY) != null;
	}
}
