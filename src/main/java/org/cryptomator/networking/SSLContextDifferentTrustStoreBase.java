package org.cryptomator.networking;

import javax.net.ssl.SSLContext;
import java.io.IOException;
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
			truststore.load(null, null);
			return SSLContextCreator.createContext(truststore, csprng);
		} catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
			throw new SSLContextBuildException(e);
		}
	}
}