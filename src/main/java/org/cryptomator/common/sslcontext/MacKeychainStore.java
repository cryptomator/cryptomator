package org.cryptomator.common.sslcontext;

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
 * SSLContextProvider for macOS using the macOS Keychain
 * <p>
 * Provided by java.base module/jmod
 */
@OperatingSystem(OperatingSystem.Value.MAC)
public class MacKeychainStore implements SSLContextProvider {

	@Override
	public SSLContext getContext(SecureRandom csprng) throws SSLContextBuildException {
		try {
			KeyStore truststore = KeyStore.getInstance("KeychainStore-ROOT");
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
