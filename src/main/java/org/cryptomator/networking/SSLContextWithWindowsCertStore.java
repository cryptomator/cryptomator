package org.cryptomator.networking;

import org.cryptomator.integrations.common.OperatingSystem;

import java.security.KeyStore;
import java.security.KeyStoreException;

@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class SSLContextWithWindowsCertStore extends SSLContextDifferentTrustStoreBase {

	@Override
	KeyStore getTruststore() throws KeyStoreException {
		return KeyStore.getInstance("WINDOWS-ROOT");
	}
}