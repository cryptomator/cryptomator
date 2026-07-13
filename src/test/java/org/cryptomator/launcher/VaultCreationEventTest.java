package org.cryptomator.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class VaultCreationEventTest {

	private static final byte[] TEMPLATE_BYTES = "a-ready-made-vault-zip".getBytes(StandardCharsets.UTF_8);
	private static final String TEMPLATE_B64 = Base64.getUrlEncoder().withoutPadding().encodeToString(TEMPLATE_BYTES);

	@Test
	@DisplayName("a valid vault/create deeplink is parsed")
	public void testValid() {
		var inTest = VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=MyVault&template=" + TEMPLATE_B64)).orElseThrow();

		Assertions.assertEquals("MyVault", inTest.name());
		Assertions.assertArrayEquals(TEMPLATE_BYTES, inTest.template());
	}

	@Test
	@DisplayName("a URL-encoded name is decoded")
	public void testUrlEncodedName() {
		var inTest = VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=My%20Vault&template=" + TEMPLATE_B64)).orElseThrow();

		Assertions.assertEquals("My Vault", inTest.name());
	}

	@Test
	@DisplayName("a non-cryptomator scheme yields empty")
	public void testWrongScheme() {
		Assertions.assertEquals(Optional.empty(), VaultCreationEvent.tryParse(URI.create("foobar://vault/create?name=MyVault&template=" + TEMPLATE_B64)));
	}

	@Test
	@DisplayName("an unknown host yields empty")
	public void testWrongHost() {
		Assertions.assertEquals(Optional.empty(), VaultCreationEvent.tryParse(URI.create("cryptomator://foo/create?name=MyVault&template=" + TEMPLATE_B64)));
	}

	@Test
	@DisplayName("an unknown path yields empty")
	public void testWrongPath() {
		Assertions.assertEquals(Optional.empty(), VaultCreationEvent.tryParse(URI.create("cryptomator://vault/bar?name=MyVault&template=" + TEMPLATE_B64)));
	}

	@Test
	@DisplayName("a matching host is recognized case-insensitively")
	public void testHostCaseInsensitive() {
		Assertions.assertTrue(VaultCreationEvent.tryParse(URI.create("cryptomator://VAULT/create?name=MyVault&template=" + TEMPLATE_B64)).isPresent());
	}

	@Test
	@DisplayName("a missing name fails")
	public void testMissingName() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?template=" + TEMPLATE_B64)));
	}

	@Test
	@DisplayName("a blank name fails")
	public void testBlankName() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=&template=" + TEMPLATE_B64)));
	}

	@Test
	@DisplayName("a missing template fails")
	public void testMissingTemplate() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=MyVault")));
	}

	@Test
	@DisplayName("an invalid Base64URL template fails")
	public void testInvalidTemplate() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=MyVault&template=@@@")));
	}

	@Test
	@DisplayName("a name containing a path separator fails")
	public void testNameWithSlash() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=foo%2Fbar&template=" + TEMPLATE_B64)));
	}

	@Test
	@DisplayName("a name with parent-dir traversal fails")
	public void testNameWithTraversal() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> VaultCreationEvent.tryParse(URI.create("cryptomator://vault/create?name=..&template=" + TEMPLATE_B64)));
	}

}
