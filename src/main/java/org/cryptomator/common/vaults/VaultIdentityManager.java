package org.cryptomator.common.vaults;

import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Manages multiple identities for a vault, supporting plausibly deniable encryption.
 * 
 * TrueCrypt-Style Design (SECURE):
 * - All vaults use: masterkey.cryptomator (single file, multiple keyslots)
 * - Cannot detect hidden vault by file presence
 * - Must try passwords to discover keyslots
 * - No .vault-identities.json (reveals nothing)
 * 
 * Previous design flaw: masterkey.bak file presence revealed hidden vault!
 * New design: All identities in same file = true plausible deniability
 */
public class VaultIdentityManager {

	private static final Logger LOG = LoggerFactory.getLogger(VaultIdentityManager.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator";

	private final Path vaultPath;
	private final MultiKeyslotFile multiKeyslotFile;
	private List<VaultIdentity> detectedIdentities;

	private VaultIdentityManager(Path vaultPath, MultiKeyslotFile multiKeyslotFile) {
		this.vaultPath = vaultPath;
		this.multiKeyslotFile = multiKeyslotFile;
		this.detectedIdentities = new ArrayList<>();
	}

	/**
	 * Load identity manager and detect identities from multi-keyslot file.
	 * NO JSON files - detection is based solely on keyslot count.
	 */
	public static VaultIdentityManager load(Path vaultPath, MultiKeyslotFile multiKeyslotFile) throws IOException {
		VaultIdentityManager manager = new VaultIdentityManager(vaultPath, multiKeyslotFile);
		manager.detectIdentities();
		return manager;
	}

	/**
	 * Detect identities by checking keyslot count in masterkey file.
	 * TrueCrypt-style: We can see how many keyslots exist, but can't tell which
	 * password unlocks which keyslot without trying.
	 */
	private void detectIdentities() throws IOException {
		detectedIdentities.clear();
		
		Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
		if (!Files.exists(masterkeyPath)) {
			LOG.warn("No masterkey file found at {}", vaultPath);
			return;
		}
		
		try {
			int keyslotCount = multiKeyslotFile.getKeyslotCount(masterkeyPath);
			
			// Primary identity (always first keyslot)
			VaultIdentity primary = VaultIdentity.createPrimary("Primary", "Main vault");
			detectedIdentities.add(primary);
			
			// Additional identities (hidden vaults)
			for (int i = 1; i < keyslotCount; i++) {
				VaultIdentity hidden = VaultIdentity.createSecondary("Hidden-" + i, "Hidden vault #" + i);
				detectedIdentities.add(hidden);
			}
			
			LOG.debug("Detected {} keyslot(s) in vault at {}", keyslotCount, vaultPath);
			
		} catch (IOException e) {
			LOG.error("Failed to detect identities", e);
			// Fall back to single identity
			VaultIdentity primary = VaultIdentity.createPrimary("Primary", "Main vault");
			detectedIdentities.add(primary);
		}
	}

	/**
	 * Save is NO-OP for stealth mode - we don't save metadata.
	 */
	public void save() throws IOException {
		// NO-OP: We don't save identity metadata for plausible deniability
		LOG.debug("Save called but skipped (stealth mode)");
	}

	/**
	 * Get all detected identities.
	 */
	public List<VaultIdentity> getIdentities() {
		return Collections.unmodifiableList(detectedIdentities);
	}

	/**
	 * Get primary identity (always first).
	 */
	public Optional<VaultIdentity> getPrimaryIdentity() {
		return detectedIdentities.stream()
				.filter(VaultIdentity::isPrimary)
				.findFirst();
	}

	/**
	 * Check if hidden vault exists (multiple keyslots in masterkey file).
	 * NOTE: This reveals that a hidden vault exists! Use with caution.
	 * In true plausible deniability scenarios, avoid calling this method.
	 */
	public boolean hasHiddenVault() {
		try {
			Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
			return multiKeyslotFile.getKeyslotCount(masterkeyPath) > 1;
		} catch (IOException e) {
			LOG.error("Failed to check for hidden vault", e);
			return false;
		}
	}

	/**
	 * Get the masterkey file path for an identity.
	 * TrueCrypt-style: All identities use the same file.
	 */
	public Path getMasterkeyPath(VaultIdentity identity) {
		// All identities use same file with multiple keyslots
		return vaultPath.resolve(MASTERKEY_FILENAME);
	}

	/**
	 * Get list of all masterkey files to try (for auto-detection).
	 * TrueCrypt-style: Only one file, but it contains multiple keyslots.
	 */
	public List<Path> getAllMasterkeyPaths() {
		// Only one masterkey file, but multiple keyslots inside
		return List.of(vaultPath.resolve(MASTERKEY_FILENAME));
	}

	/**
	 * Delete hidden vault (remove keyslot from multi-keyslot file).
	 * WARNING: Requires the password of the hidden vault to remove it.
	 * 
	 * @param hiddenPassword Password of the hidden vault to remove
	 * @return true if a keyslot was removed
	 */
	public boolean removeHiddenVault(CharSequence hiddenPassword) {
		try {
			Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
			Path hiddenVaultConfig = vaultPath.resolve("vault.bak");
			
			boolean keyslotRemoved = multiKeyslotFile.removeKeyslot(masterkeyPath, hiddenPassword);
			boolean configDeleted = Files.deleteIfExists(hiddenVaultConfig);
			
			if (keyslotRemoved) {
				detectIdentities(); // Refresh detection
				LOG.info("Removed hidden vault from {} (keyslot: {}, config: {})", 
						vaultPath, keyslotRemoved, configDeleted);
			}
			return keyslotRemoved;
		} catch (IOException e) {
			LOG.error("Failed to remove hidden vault", e);
			return false;
		}
	}
	
	/**
	 * Get the multi-keyslot file handler.
	 */
	public MultiKeyslotFile getMultiKeyslotFile() {
		return multiKeyslotFile;
	}
}
