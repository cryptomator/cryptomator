package org.cryptomator.common.networking;

import org.cryptomator.integrations.common.OperatingSystem;

import java.security.KeyStore;
import java.security.KeyStoreException;

/**
 * SSLContextProvider for Windows using the Windows certificate store as trust store
 * <p>
 * In order to work, the jdk.crypto.mscapi jmod is needed
 */
@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class SSLContextWithWindowsCertStore extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	@Override
	KeyStore getTruststore() throws KeyStoreException {
		return KeyStore.getInstance("WINDOWS-ROOT");
	}

}
