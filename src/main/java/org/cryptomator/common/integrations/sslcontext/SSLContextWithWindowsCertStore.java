package org.cryptomator.common.integrations.sslcontext;

import org.cryptomator.integrations.common.OperatingSystem;

import java.security.KeyStore;
import java.security.KeyStoreException;

/**
 * SSLContextProvider for Windows using the Windows certificate store
 * <p>
 * Provided by jdk.crypto.mscapi jmod
 */
@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class SSLContextWithWindowsCertStore extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	@Override
	KeyStore getTruststore() throws KeyStoreException {
		return KeyStore.getInstance("WINDOWS-ROOT");
	}

}
