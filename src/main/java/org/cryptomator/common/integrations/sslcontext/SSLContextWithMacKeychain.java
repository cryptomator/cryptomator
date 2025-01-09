package org.cryptomator.common.integrations.sslcontext;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * SSLContextProvider for macOS using the macOS Keychain as truststore
 */
@OperatingSystem(OperatingSystem.Value.MAC)
public class SSLContextWithMacKeychain extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		return KeyStore.getInstance("KeychainStore-ROOT");
	}
}
