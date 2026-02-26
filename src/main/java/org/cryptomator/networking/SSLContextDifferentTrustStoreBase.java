package org.cryptomator.networking;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

abstract class SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	abstract KeyStore getTruststore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException;

	@Override
	public SSLContext getContext(SecureRandom csprng) throws SSLContextBuildException {
		try {
			KeyStore truststore = getTruststore();
			ensureLoaded(truststore);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(truststore);

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), csprng);
			return context;
		} catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | KeyManagementException | IOException e) {
			throw new SSLContextBuildException(e);
		}
	}

	static void ensureLoaded(KeyStore truststore) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		try {
			truststore.aliases();
		} catch (KeyStoreException e) {
			// Not initialized yet (e.g. custom KeyStore SPI); initialize without replacing preloaded stores.
			truststore.load(null, null);
		}
	}
}
