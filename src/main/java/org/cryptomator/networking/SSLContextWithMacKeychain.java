// SSLContextWithMacKeychain.java
package org.cryptomator.networking;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@OperatingSystem(OperatingSystem.Value.MAC)
public class SSLContextWithMacKeychain extends SSLContextDifferentTrustStoreBase {

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		return KeyStore.getInstance("KeychainStore-ROOT");
	}
}