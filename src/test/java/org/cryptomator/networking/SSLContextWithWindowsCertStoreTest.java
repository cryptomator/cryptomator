package org.cryptomator.networking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

public class SSLContextWithWindowsCertStoreTest {

	private static final String JAVA_HOME_PROP = "java.home";
	private static final String TRUSTSTORE_PASSWORD_PROP = "javax.net.ssl.trustStorePassword";

	@TempDir
	Path tmpDir;

	@Test
	public void testGetCaCertsStoreByPropertiesReturnsNullIfNoCandidateExists() {
		var props = propsFor(tmpDir, null);

		var inTest = new SSLContextWithWindowsCertStore();

		Assertions.assertNull(inTest.getCaCertsStoreByProperties(props));
	}

	@Test
	public void testGetCaCertsStoreByPropertiesLoadsLibSecurityCacertsByDefault() throws Exception {
		var cacerts = tmpDir.resolve("lib/security/cacerts");
		writePkcs12Keystore(cacerts, "changeit".toCharArray());
		var props = propsFor(tmpDir, null);

		var inTest = new SSLContextWithWindowsCertStore();

		Assertions.assertNotNull(inTest.getCaCertsStoreByProperties(props));
	}

	@Test
	public void testGetCaCertsStoreByPropertiesTriesSecondCandidateAfterFirstFails() throws Exception {
		var invalidLibCacerts = tmpDir.resolve("lib/security/cacerts");
		Files.createDirectories(invalidLibCacerts.getParent());
		Files.writeString(invalidLibCacerts, "not a keystore");
		var confCacerts = tmpDir.resolve("conf/security/cacerts");
		writePkcs12Keystore(confCacerts, "changeit".toCharArray());
		var props = propsFor(tmpDir, null);

		var inTest = new SSLContextWithWindowsCertStore();

		Assertions.assertNotNull(inTest.getCaCertsStoreByProperties(props));
	}

	@Test
	public void testGetCaCertsStoreByPropertiesReturnsNullOnWrongPassword() throws Exception {
		var cacerts = tmpDir.resolve("lib/security/cacerts");
		writePkcs12Keystore(cacerts, "changeit".toCharArray());
		var props = propsFor(tmpDir, "wrong-password");

		var inTest = new SSLContextWithWindowsCertStore();

		Assertions.assertNull(inTest.getCaCertsStoreByProperties(props));
	}

	@Test
	public void testGetCaCertsStoreByPropertiesUsesCustomPasswordProperty() throws Exception {
		var cacerts = tmpDir.resolve("lib/security/cacerts");
		writePkcs12Keystore(cacerts, "custom-password".toCharArray());
		var props = propsFor(tmpDir, "custom-password");

		var inTest = new SSLContextWithWindowsCertStore();

		Assertions.assertNotNull(inTest.getCaCertsStoreByProperties(props));
	}

	private static void writePkcs12Keystore(Path target, char[] password) throws CertificateException, IOException, NoSuchAlgorithmException, java.security.KeyStoreException {
		Files.createDirectories(target.getParent());
		var keystore = KeyStore.getInstance("PKCS12");
		keystore.load(null, null);
		try (var out = Files.newOutputStream(target)) {
			keystore.store(out, password);
		}
	}

	private static Properties propsFor(Path javaHome, String truststorePassword) {
		var props = new Properties();
		props.setProperty(JAVA_HOME_PROP, javaHome.toString());
		if (truststorePassword != null) {
			props.setProperty(TRUSTSTORE_PASSWORD_PROP, truststorePassword);
		}
		return props;
	}

}
