package org.cryptomator.networking;

import org.cryptomator.common.Nullable;
import org.cryptomator.integrations.common.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
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

	private static final Logger LOG = LoggerFactory.getLogger(SSLContextWithWindowsCertStore.class);
	private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit"; //default JDK cacerts password

	@Override
	KeyStore getTruststore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		var windowsKeyStore = KeyStore.getInstance("WINDOWS-ROOT");
		var jdkKeyStore = getShippedCaCertsStore();
		if (jdkKeyStore == null) {
			return windowsKeyStore;
		}

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

	@Nullable
	KeyStore getShippedCaCertsStore() {
		var javaHome = Path.of(System.getProperty("java.home"));
		var trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword", DEFAULT_TRUSTSTORE_PASSWORD).toCharArray();
		for (var candidate : List.of(javaHome.resolve("lib/security/cacerts"), javaHome.resolve("conf/security/cacerts"))) {
			try {
				if (Files.isRegularFile(candidate)) {
					return KeyStore.getInstance(candidate.toFile(), trustStorePassword);
				}
			} catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
				LOG.info("Unable to load fallback cacerts {} file. Skipping fallback.", candidate, e);
			}
		}
		return null;
	}

}
