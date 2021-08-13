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

	@Test
	@Disabled // only to be run manually
	public void testRetrieveToken() throws Exception {
		var hubConfig = new HubConfig();
		hubConfig.authEndpoint = "http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/auth";
		hubConfig.tokenEndpoint = "http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/token";
		hubConfig.clientId = "cryptomator-hub";
		hubConfig.unlockSuccessUrl = "http://localhost:3000/#/unlock-success";
		hubConfig.unlockErrorUrl = "http://localhost:3000/#/unlock-error";

		try (var authFlow = AuthFlow.init(hubConfig)) {
			var token = authFlow.run(uri -> {
				LOG.info("Visit {} to authenticate", uri);
			});
			LOG.info("Received token {}", token);
			Assertions.assertNotNull(token);
		}
	}

}