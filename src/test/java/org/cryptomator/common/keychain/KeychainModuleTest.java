package org.cryptomator.common.keychain;

import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class KeychainModuleTest {

	private final KeychainAccessProvider firstProvider = new MapKeychainAccess();
	private final KeychainAccessProvider secondProvider = new OtherKeychainAccess();

	@Test
	public void testSelectsStoredProvider() {
		var provider = KeychainModule.selectProvider(OtherKeychainAccess.class.getName(), List.of(firstProvider, secondProvider));

		Assertions.assertSame(secondProvider, provider);
	}

	@Test
	public void testFallsBackToFirstProviderIfStoredOneIsUnavailable() {
		var provider = KeychainModule.selectProvider("org.example.UnavailableKeychainAccess", List.of(firstProvider, secondProvider));

		Assertions.assertSame(firstProvider, provider);
	}

	@Test
	public void testSelectsNoProviderIfNoneIsAvailable() {
		var provider = KeychainModule.selectProvider(OtherKeychainAccess.class.getName(), List.of());

		Assertions.assertNull(provider);
	}

	private static class OtherKeychainAccess extends MapKeychainAccess {}

}
