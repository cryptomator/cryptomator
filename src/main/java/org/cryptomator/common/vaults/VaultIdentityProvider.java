package org.cryptomator.common.vaults;

import org.cryptomator.common.keychain.MultiKeyslotFile;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Provider for accessing vault identity information.
 * This is injected per-vault scope to provide identity management capabilities.
 */
@PerVault
public class VaultIdentityProvider {

	private final Path vaultPath;
	private final MultiKeyslotFile multiKeyslotFile;
	private VaultIdentityManager manager;

	@Inject
	public VaultIdentityProvider(@PerVault Path vaultPath, MultiKeyslotFile multiKeyslotFile) {
		this.vaultPath = vaultPath;
		this.multiKeyslotFile = multiKeyslotFile;
	}

	/**
	 * Get or load the identity manager.
	 */
	public synchronized VaultIdentityManager getManager() throws IOException {
		if (manager == null) {
			manager = VaultIdentityManager.load(vaultPath, multiKeyslotFile);
		}
		return manager;
	}

	/**
	 * Reload the identity manager from disk.
	 */
	public synchronized void reload() throws IOException {
		manager = VaultIdentityManager.load(vaultPath, multiKeyslotFile);
	}

	/**
	 * Clear cached manager.
	 */
	public synchronized void clear() {
		manager = null;
	}
}
