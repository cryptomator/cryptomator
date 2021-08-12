package org.cryptomator.ui.keyloading.hub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class AuthFlowIntegrationTest {

	static {
		System.setProperty("LOGLEVEL", "INFO");
	}

	private static final Logger LOG = LoggerFactory.getLogger(AuthFlowIntegrationTest.class);
	private static final URI AUTH_URI = URI.create("http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/auth");
	private static final URI TOKEN_URI = URI.create("http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/token");
	private static final String CLIENT_ID = "cryptomator-hub";

	@Test
	@Disabled // only to be run manually
	public void testRetrieveToken() throws Exception {
		try (var authFlow = AuthFlow.init(AUTH_URI, TOKEN_URI, CLIENT_ID)) {
			var token = authFlow.run(uri -> {
				LOG.info("Visit {} to authenticate", uri);
			});
			LOG.info("Received token {}", token);
			Assertions.assertNotNull(token);
		}
	}

}