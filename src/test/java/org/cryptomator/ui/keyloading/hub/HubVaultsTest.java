package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultConfigCache;
import org.cryptomator.cryptofs.VaultConfig.UnverifiedVaultConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HubVaultsTest {

	private static final UUID VAULT_ID = UUID.fromString("d3a1f0b2-7c4e-4a1d-9f3b-2e5c6a7b8c9d");
	private static final UUID OTHER_VAULT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

	@Test
	@DisplayName("the hub vault with the requested id is found")
	public void testFindsMatchingVault() throws IOException {
		var wanted = hubVault(VAULT_ID);
		var vaults = List.of(hubVault(OTHER_VAULT_ID), wanted);

		Assertions.assertEquals(Optional.of(wanted), HubVaults.findByVaultId(vaults, VAULT_ID));
	}

	@Test
	@DisplayName("no vault with the requested id yields empty")
	public void testNoMatch() throws IOException {
		var vaults = List.of(hubVault(OTHER_VAULT_ID));

		Assertions.assertEquals(Optional.empty(), HubVaults.findByVaultId(vaults, VAULT_ID));
	}

	@Test
	@DisplayName("a password vault never matches, even carrying the same id")
	public void testIgnoresPasswordVault() throws IOException {
		var vaults = List.of(vault("masterkeyfile:masterkey.cryptomator", VAULT_ID));

		Assertions.assertEquals(Optional.empty(), HubVaults.findByVaultId(vaults, VAULT_ID));
	}

	@Test
	@DisplayName("a vault whose config cannot be read is skipped, not fatal")
	public void testSkipsUnreadableVault() throws IOException {
		// e.g. a vault on a network drive that is currently offline - it must not hide a vault further down the list
		var wanted = hubVault(VAULT_ID);
		var vaults = List.of(unreadableVault(), wanted);

		Assertions.assertEquals(Optional.of(wanted), HubVaults.findByVaultId(vaults, VAULT_ID));
	}


	// setup/mock provider

	private static Vault hubVault(UUID vaultId) throws IOException {
		return vault("hub+https://hub.example.com/api/vaults/" + vaultId, vaultId);
	}

	private static Vault vault(String keyId, UUID vaultId) throws IOException {
		var configCache = mock(VaultConfigCache.class);
		var config = mockConfig(keyId, vaultId);
		when(configCache.get()).thenReturn(config);
		return vaultWith(configCache);
	}

	private static Vault unreadableVault() throws IOException {
		var configCache = mock(VaultConfigCache.class);
		when(configCache.get()).thenThrow(new IOException("vault directory unavailable"));
		return vaultWith(configCache);
	}

	private static Vault vaultWith(VaultConfigCache configCache) {
		var vault = mock(Vault.class);
		when(vault.getVaultConfigCache()).thenReturn(configCache);
		return vault;
	}

	private static UnverifiedVaultConfig mockConfig(String keyId, UUID vaultId) {
		var mock = mock(UnverifiedVaultConfig.class);
		when(mock.getKeyId()).thenReturn(URI.create(keyId));
		when(mock.allegedVaultId()).thenReturn(vaultId.toString());
		return mock;
	}

}
