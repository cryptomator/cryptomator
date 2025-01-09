package org.cryptomator.common.integrations.sslcontext;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

@OperatingSystem(OperatingSystem.Value.LINUX)
public class SSLContextWithPKCS12File implements SSLContextProvider {

	private static final String CERT_FILE_LOCATION_PROPERTY = "org.cryptomator.common.integrations.sslcontext.pkcs12file";

	@Override
	public SSLContext getContext(SecureRandom csprng) throws SSLContextBuildException {
		try {
			var pkcs12FilePath = Path.of(System.getProperty(CERT_FILE_LOCATION_PROPERTY));
			KeyStore truststore = KeyStore.getInstance(pkcs12FilePath.toFile(), new char[]{});
			truststore.load(null, null);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(truststore);

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), csprng);
			return context;
		} catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | KeyManagementException | IOException | InvalidPathException e) {
			throw new SSLContextBuildException(e);
		}
	}

	@CheckAvailability
	static boolean isSupported() {
		return System.getProperty(CERT_FILE_LOCATION_PROPERTY) != null;
	}
}
