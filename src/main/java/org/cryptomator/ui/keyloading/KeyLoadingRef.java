package org.cryptomator.ui.keyloading;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;

import java.io.IOException;
import java.net.URI;

/**
 * Identifies the vault a key is being loaded for, independently of whether that vault exists on this machine.
 * <p>
 * Key loading needs the (unverified) vault config selecting the strategy and addresses
 * the vault within the strategy, and the display name titles the windows.
 * <p>
 * The config is <em>unverified</em>: its signature is keyed on the masterkey, which is exactly what key loading is
 * about to obtain.
 *
 * @param vaultConfig the vault's unverified config
 * @param displayName the vault's name, as shown to the user
 */
public record KeyLoadingRef(VaultConfig.UnverifiedVaultConfig vaultConfig, String displayName) {

	/**
	 * Describes a vault that is already set up on this machine.
	 *
	 * @param vault the vault to load a key for
	 * @throws IOException if the vault's config cannot be read
	 */
	public static KeyLoadingRef forVault(Vault vault) throws IOException {
		return new KeyLoadingRef(vault.getVaultConfigCache().get(), vault.getDisplayName());
	}

	/**
	 * The key id, whose scheme selects the key loading strategy.
	 */
	public URI keyId() {
		return vaultConfig.getKeyId();
	}

	/**
	 * The vault's id, i.e. how the vault is addressed within its Hub instance.
	 * <p>
	 * Read from the config's {@code jti} claim, which is the authoritative source: the key id carries the same id in its
	 * trailing path segment, but only its <em>scheme</em> is a source of truth here.
	 */
	public String vaultId() {
		return vaultConfig.allegedVaultId();
	}

}
