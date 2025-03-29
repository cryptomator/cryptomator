package org.cryptomator.networking;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SSLContextCreator {

	public static SSLContext createContext(KeyStore truststore, SecureRandom csprng) throws SSLContextProvider.SSLContextBuildException {
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(truststore);

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), csprng);
			return context;
		} catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
			throw new SSLContextProvider.SSLContextBuildException(e);
		}
	}
}