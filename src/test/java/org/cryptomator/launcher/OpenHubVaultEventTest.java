package org.cryptomator.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class OpenHubVaultEventTest {

	private static final String VAULT_ID = "d3a1f0b2-7c4e-4a1d-9f3b-2e5c6a7b8c9d";
	private static final String KEY_ID = "hub+https://hub.example.com/api/vaults/" + VAULT_ID;
	private static final String TOKEN = hubVaultConfig(KEY_ID);

	@Test
	@DisplayName("a valid vault/open deeplink is parsed")
	public void testValid() {
		var inTest = OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + TOKEN)).orElseThrow();

		Assertions.assertEquals(UUID.fromString(VAULT_ID), inTest.vaultId());
		Assertions.assertEquals(URI.create(KEY_ID), inTest.vaultConfig().getKeyId());
	}

	@Test
	@DisplayName("parameters in the query instead of the fragment are not accepted")
	public void testQueryParamsRejected() {
		var uri = URI.create("org.cryptomator://vault/open?vaultConfig=" + TOKEN);

		Assertions.assertThrows(IllegalArgumentException.class, () -> OpenHubVaultEvent.tryParse(uri));
	}

	@Test
	@DisplayName("an encoded separator inside a value cannot forge another parameter")
	public void testNoParameterInjectionViaEncodedSeparator() {
		// '%26' must stay part of the first value; decoding the fragment before splitting would turn it into a real
		// separator and smuggle in a 'vaultConfig' the link never carried - hence getRawFragment(), decoding per value.
		var uri = URI.create("org.cryptomator://vault/open#other=a%26vaultConfig=" + TOKEN);

		Assertions.assertThrows(IllegalArgumentException.class, () -> OpenHubVaultEvent.tryParse(uri));
	}

	@Test
	@DisplayName("a config exceeding the size limit is rejected")
	public void testExceedsSizeLimit() {
		var oversized = "a".repeat(8193);

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + oversized)));
	}

	@Test
	@DisplayName("a config that is not a decodable token is rejected")
	public void testNotAToken() {
		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=not-a-jwt")));
	}

	@Test
	@DisplayName("a config without a hub key id is rejected")
	public void testNotAHubVault() {
		var token = hubVaultConfig("masterkeyfile:masterkey.cryptomator");

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)));
	}

	@Test
	@DisplayName("a config without a hub header is rejected")
	public void testNoHubHeader() {
		var token = vaultConfig(KEY_ID, VAULT_ID, null);

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)));
	}

	@ParameterizedTest
	@DisplayName("a vault id that is not a uuid is rejected")
	@ValueSource(strings = { //
			"not-a-uuid", //
			"", // absent
			"../../evil" // must never reach the api/vaults/{vaultId}/... request path
	})
	public void testInvalidVaultId(String vaultId) {
		var token = vaultConfig(KEY_ID, vaultId, hubHeader("https://hub.example.com/api", "https://login.example.com/auth"));

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)));
	}

	@Test
	@DisplayName("a vault id disagreeing with the key id is rejected")
	public void testVaultIdMismatch() {
		// Hub writes the same id into both, so a config where they differ is forged or broken
		var token = vaultConfig(KEY_ID, "11111111-2222-3333-4444-555555555555", //
				hubHeader("https://hub.example.com/api", "https://login.example.com/auth"));

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)));
	}

	@ParameterizedTest
	@DisplayName("an unusable hub endpoint is rejected")
	@ValueSource(strings = { //
			"ftp://hub.example.com/api", // neither http nor https
			"/api", // not absolute
			"https:///api" // no host
	})
	public void testUnusableApiBaseUrl(String apiBaseUrl) {
		var token = vaultConfig(KEY_ID, VAULT_ID, hubHeader(apiBaseUrl, "https://login.example.com/auth"));

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)));
	}

	@Test
	@DisplayName("an unusable auth endpoint is rejected")
	public void testUnusableAuthEndpoint() {
		var token = vaultConfig(KEY_ID, VAULT_ID, hubHeader("https://hub.example.com/api", "not a url"));

		Assertions.assertThrows(IllegalArgumentException.class, //
				() -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)));
	}

	@Test
	@DisplayName("an http endpoint is accepted here, host trust decides later")
	public void testHttpEndpointAccepted() {
		var token = vaultConfig(KEY_ID, VAULT_ID, hubHeader("http://localhost:8080/api", "http://localhost:8080/auth"));

		Assertions.assertTrue(OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=" + token)).isPresent());
	}

	@Test
	@DisplayName("a non-cryptomator scheme yields empty")
	public void testWrongScheme() {
		Assertions.assertEquals(Optional.empty(), OpenHubVaultEvent.tryParse(URI.create("foobar://vault/open#vaultConfig=" + TOKEN)));
	}

	@Test
	@DisplayName("the bare cryptomator scheme is no longer recognized")
	public void testLegacyScheme() {
		Assertions.assertEquals(Optional.empty(), OpenHubVaultEvent.tryParse(URI.create("cryptomator://vault/open#vaultConfig=" + TOKEN)));
	}

	@Test
	@DisplayName("an unknown host yields empty")
	public void testWrongHost() {
		Assertions.assertEquals(Optional.empty(), OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://foo/open#vaultConfig=" + TOKEN)));
	}

	@Test
	@DisplayName("an unknown path yields empty")
	public void testWrongPath() {
		Assertions.assertEquals(Optional.empty(), OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/create#vaultConfig=" + TOKEN)));
	}

	@Test
	@DisplayName("a matching host is recognized case-insensitively")
	public void testHostCaseInsensitive() {
		Assertions.assertTrue(OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://VAULT/open#vaultConfig=" + TOKEN)).isPresent());
	}

	@Test
	@DisplayName("a missing config fails")
	public void testMissingConfig() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open")));
	}

	@Test
	@DisplayName("a blank config fails")
	public void testBlankConfig() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> OpenHubVaultEvent.tryParse(URI.create("org.cryptomator://vault/open#vaultConfig=")));
	}

	private static String hubHeader(String apiBaseUrl, String authEndpoint) {
		return """
				,
				"hub": {
					"clientId":"cryptomator",\
					"authEndpoint":"%s",\
					"tokenEndpoint":"https://login.example.com/token",\
					"authSuccessUrl":"https://hub.example.com/app/unlock-success",\
					"authErrorUrl":"https://hub.example.com/app/unlock-error",\
					"apiBaseUrl":"%s"
				}""".formatted(authEndpoint, apiBaseUrl);
	}

	private static String hubVaultConfig(String keyId) {
		return vaultConfig(keyId, VAULT_ID, hubHeader("https://hub.example.com/api", "https://login.example.com/auth"));
	}

	/**
	 * Builds a vault config token. Its signature is keyed on the masterkey, which the deeplink never carries, so a dummy
	 * signature is exactly what the parser operates on.
	 */
	private static String vaultConfig(String keyId, String vaultId, String extraHeaderFields) {
		var header = """
				{ "kid":"%s",\
				  "typ":"JWT",\
				  "alg":"HS256"\
				  %s
				  }""".formatted(keyId, extraHeaderFields == null ? "" : extraHeaderFields);
		var payload = """
				{ "jti":"%s",\
				  "format":8,\
				  "cipherCombo":"SIV_GCM",\
				  "shorteningThreshold":220\
				  }""".formatted(vaultId);
		var encoder = Base64.getUrlEncoder().withoutPadding();
		return encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) //
				+ "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) //
				+ "." + encoder.encodeToString("signature".getBytes(StandardCharsets.UTF_8));
	}

}
