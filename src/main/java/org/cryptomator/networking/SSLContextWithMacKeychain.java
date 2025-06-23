package org.cryptomator.networking;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;

/**
 * SSLContextProvider for macOS using the macOS Keychain as truststore
 */
@OperatingSystem(OperatingSystem.Value.MAC)
public class SSLContextWithMacKeychain extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		var userKeyStore = KeyStore.getInstance("KeychainStore");
		var systemRootKeyStore = KeyStore.getInstance("KeychainStore-ROOT");
		userKeyStore.load(null);
		systemRootKeyStore.load(null);
		try {
			CombinedKeyStoreSpi spi = CombinedKeyStoreSpi.create(userKeyStore, systemRootKeyStore);
			Provider dummyProvider = new Provider("CombinedKeyStoreProvider", "1.0", "Provides a combined, read-only KeyStore") {};
			return new KeyStore(spi, dummyProvider, "CombinedKeyStoreProvider") {};
		} catch (IllegalArgumentException e) {
			throw new KeyStoreException(e);
		}
	}
}
