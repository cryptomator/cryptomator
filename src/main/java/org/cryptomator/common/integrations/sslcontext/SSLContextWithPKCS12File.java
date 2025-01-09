package org.cryptomator.common.integrations.sslcontext;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@OperatingSystem(OperatingSystem.Value.LINUX)
public class SSLContextWithPKCS12File extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	private static final String CERT_FILE_LOCATION_PROPERTY = "org.cryptomator.common.integrations.sslcontext.pkcs12file";

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		var pkcs12FilePath = Path.of(System.getProperty(CERT_FILE_LOCATION_PROPERTY));
		return KeyStore.getInstance(pkcs12FilePath.toFile(), new char[]{});
	}

	@CheckAvailability
	static boolean isSupported() {
		return System.getProperty(CERT_FILE_LOCATION_PROPERTY) != null;
	}
}
