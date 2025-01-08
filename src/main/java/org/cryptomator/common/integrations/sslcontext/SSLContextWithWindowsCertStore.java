package org.cryptomator.common.integrations.sslcontext;

import org.cryptomator.integrations.common.OperatingSystem;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

/**
 * SSLContextProvider for Windows using the Windows certificate store
 * <p>
 * Provided by jdk.crypto.mscapi jmod
 */
@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class SSLContextWithWindowsCertStore implements SSLContextProvider {

	@Override
	public SSLContext getContext(SecureRandom csprng) throws SSLContextBuildException {
		try {
			KeyStore truststore = KeyStore.getInstance("WINDOWS-ROOT");
			truststore.load(null, null);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(truststore);

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), csprng);
			return context;
		} catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | KeyManagementException | IOException e) {
			throw new SSLContextBuildException(e);
		}
	}
}
