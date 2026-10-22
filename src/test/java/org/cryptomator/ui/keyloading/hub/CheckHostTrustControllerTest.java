package org.cryptomator.ui.keyloading.hub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Set;

class CheckHostTrustControllerTest {

	@ParameterizedTest
	@CsvSource({
			"https://auth.example.com, https://hub.example.com, true",
			"https://hub.example.com, https://hub.example.com, true",
			"https://auth.example.com, https://auth.example.com, true",
			"https://auth.example.com, https://wrong.example.com, false",
			"https://wrong.example.com, https://wrong.example.com, false"
	})
	void testContainsAllowedHosts(String apiBase, String authEndpoint, boolean expectedResult) {
		var hubConfig = new HubConfig();
		hubConfig.apiBaseUrl = apiBase;
		hubConfig.authEndpoint = authEndpoint;
		var controller = new CheckHostTrustController(Mockito.mock(), hubConfig, Mockito.mock(), Mockito.mock(), Mockito.mock(), Mockito.mock(), Mockito.mock(), Mockito.mock());

		var actualResult = controller.containsAllowedHosts(Set.of("https://auth.example.com", "https://hub.example.com"));

		Assertions.assertEquals(expectedResult, actualResult);
	}

	@ParameterizedTest
	@CsvSource({
			"https://example.com, https://example.com",
			"https://example.com/foo/bar, https://example.com",
			"https://example.com:8080, https://example.com:8080",
			"https://user@example.com:8080/foo/bar, https://example.com:8080",
			"https://user@example.com:443/foo/bar, https://example.com:443",
			"http://user@example.com:80/foo/bar?foo=bar, http://example.com:80",
			"http://user@example.com:8080/foo/bar?foo=bar, http://example.com:8080"
	})
	void testGetAuthority(String input, String expected) {
		var actual = CheckHostTrustController.getAuthority(input);

		Assertions.assertEquals(expected, actual);
	}

}