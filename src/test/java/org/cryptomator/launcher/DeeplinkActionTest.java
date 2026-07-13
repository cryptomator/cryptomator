package org.cryptomator.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

public class DeeplinkActionTest {

	@Test
	@DisplayName("cryptomator://vault/create maps to VAULT_CREATE")
	public void testVaultCreateMatches() {
		var result = DeeplinkAction.match(URI.create("cryptomator://vault/create?name=foo&template=bar"));

		Assertions.assertEquals(Optional.of(DeeplinkAction.VAULT_CREATE), result);
	}

	@Test
	@DisplayName("host matching is case-insensitive")
	public void testHostCaseInsensitive() {
		var result = DeeplinkAction.match(URI.create("cryptomator://VAULT/create"));

		Assertions.assertEquals(Optional.of(DeeplinkAction.VAULT_CREATE), result);
	}

	@Test
	@DisplayName("an unknown host yields empty")
	public void testUnknownHost() {
		Assertions.assertEquals(Optional.empty(), DeeplinkAction.match(URI.create("cryptomator://foo/create")));
	}

	@Test
	@DisplayName("an unknown path yields empty")
	public void testUnknownPath() {
		Assertions.assertEquals(Optional.empty(), DeeplinkAction.match(URI.create("cryptomator://vault/bar")));
	}

}
