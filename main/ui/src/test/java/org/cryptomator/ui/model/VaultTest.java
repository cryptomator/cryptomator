package org.cryptomator.ui.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VaultTest {

	@Test
	public void testNormalize() throws Exception {
		assertEquals("_", Vault.normalize(" "));
		assertEquals("a", Vault.normalize("ä"));
		assertEquals("C", Vault.normalize("Ĉ"));
		assertEquals("_", Vault.normalize(":"));
		assertEquals("", Vault.normalize("汉语"));
	}

}
