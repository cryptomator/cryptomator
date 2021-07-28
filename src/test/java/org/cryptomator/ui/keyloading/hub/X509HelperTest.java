package org.cryptomator.ui.keyloading.hub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;

public class X509HelperTest {

	@Test
	public void testCreateCert() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, InvalidAlgorithmParameterException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		keyGen.initialize(new ECGenParameterSpec("secp256r1"));
		var keyPair = keyGen.generateKeyPair();
		var cert = X509Helper.createSelfSignedCert(keyPair, "SHA256withECDSA");
		Assertions.assertNotNull(cert);
	}

}