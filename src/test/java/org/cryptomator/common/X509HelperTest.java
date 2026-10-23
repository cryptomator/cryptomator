package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class X509HelperTest {

	@Test
	public void testExtractCommonNameFromMultiValuedRdn() {
		var cert = Mockito.mock(X509Certificate.class);
		Mockito.doReturn(new X500Principal("OU=Licensing+CN=License Issuer CA (Test), O=Skymatic GmbH, C=DE")).when(cert).getSubjectX500Principal();

		Optional<String> cn = X509Helper.extractCommonName(cert);

		Assertions.assertEquals(Optional.of("License Issuer CA (Test)"), cn);
	}

}
