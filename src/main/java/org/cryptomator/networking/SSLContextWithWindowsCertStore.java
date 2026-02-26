package org.cryptomator.networking;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * SSLContextProvider for Windows using the Windows certificate store as trust store and the bundled JDK cacerts as fallback
 * <p>
 * In order to work, the jdk.crypto.mscapi jmod is needed
 */
@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class SSLContextWithWindowsCertStore extends SSLContextDifferentTrustStoreBase implements SSLContextProvider {

	private static final String DEFAULT_TRUSTSTORE_PASSWORD = "";

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		var windowsKeyStore = KeyStore.getInstance("WINDOWS-ROOT");
		var jdkKeyStore = getShippedCaCertsStore();
		ensureLoaded(windowsKeyStore);
		ensureLoaded(jdkKeyStore);
		try {
			CombinedKeyStoreSpi spi = CombinedKeyStoreSpi.create(windowsKeyStore, jdkKeyStore);
			Provider dummyProvider = new Provider("CombinedKeyStoreProvider", "1.0", "Provides a combined, read-only KeyStore") {};
			return new KeyStore(spi, dummyProvider, "CombinedKeyStoreProvider") {};
		} catch (IllegalArgumentException e) {
			throw new KeyStoreException(e);
		}
	}

	KeyStore getShippedCaCertsStore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
		var javaHome = Path.of(System.getProperty("java.home"));
		var trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword", DEFAULT_TRUSTSTORE_PASSWORD).toCharArray();
		for (var candidate : List.of(javaHome.resolve("lib/security/cacerts"), javaHome.resolve("conf/security/cacerts"))) {
			if (Files.isRegularFile(candidate)) {
				return KeyStore.getInstance(candidate.toFile(), trustStorePassword);
			}
		}
		throw new NoSuchFileException("Could not locate cacerts below java.home: " + javaHome);
	}

}
