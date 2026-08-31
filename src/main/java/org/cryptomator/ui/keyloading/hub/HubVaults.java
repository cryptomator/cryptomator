package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Locates Hub vaults among the vaults set up on this machine.
 * <p>
 * Hub manages a vault's key, not where it lives or how it is laid out, so the local vault list is the only place that
 * can answer whether a given Hub vault is already set up here.
 */
public final class HubVaults {

	private static final Logger LOG = LoggerFactory.getLogger(HubVaults.class);

	private HubVaults() {
	}

	/**
	 * Finds the Hub vault with the given id.
	 * <p>
	 * A vault whose config cannot be read - e.g. because it sits on storage that is currently unavailable - is skipped
	 * rather than failing the lookup: one unreachable vault must not prevent finding a different one.
	 *
	 * @param vaults     the vaults set up on this machine
	 * @param hubVaultId the vault's id within its Hub instance
	 * @return the local vault, or empty if none of them is that Hub vault
	 */
	public static Optional<Vault> findByVaultId(Collection<Vault> vaults, UUID hubVaultId) {
		return vaults.stream() //
				.filter(vault -> hasVaultId(vault, hubVaultId)) //
				.findAny();
	}

	private static boolean hasVaultId(Vault vault, UUID hubVaultId) {
		try {
			return hasVaultId(vault.getVaultConfigCache().get(), hubVaultId);
		} catch (IOException e) {
			LOG.debug("Skipping vault {} while looking for hub vault {}, its config is not readable.", vault.getPath(), hubVaultId);
			return false;
		}
	}

	private static boolean hasVaultId(VaultConfig.UnverifiedVaultConfig config, UUID hubVaultId) {
		var keyIdScheme = config.getKeyId().getScheme();
		if (keyIdScheme == null || !keyIdScheme.startsWith(HubKeyLoadingStrategy.SCHEME_PREFIX)) {
			return false; //not a hub vault, so it cannot be the one we are looking for
		}
		return hubVaultId.toString().equalsIgnoreCase(config.allegedVaultId());
	}

}
