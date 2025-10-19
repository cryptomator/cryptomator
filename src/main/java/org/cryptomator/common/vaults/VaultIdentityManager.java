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
 * - CANNOT detect how many identities exist without trying passwords
 * - NO enumeration of identities - violates plausible deniability
 * - Unlock process discovers identity by password match
 * - No .vault-identities.json (reveals nothing)
 * 
 * Security principle: Identity count is unknowable without passwords.
 * UI should NOT show "you have N identities" - that defeats the purpose!
 */
public class VaultIdentityManager {

	private static final Logger LOG = LoggerFactory.getLogger(VaultIdentityManager.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator";

	private final Path vaultPath;
	private final MultiKeyslotFile multiKeyslotFile;

	private VaultIdentityManager(Path vaultPath, MultiKeyslotFile multiKeyslotFile) {
		this.vaultPath = vaultPath;
		this.multiKeyslotFile = multiKeyslotFile;
	}

	/**
	 * Load identity manager for a vault.
	 * Does NOT detect/enumerate identities (that would violate plausible deniability).
	 */
	public static VaultIdentityManager load(Path vaultPath, MultiKeyslotFile multiKeyslotFile) throws IOException {
		VaultIdentityManager manager = new VaultIdentityManager(vaultPath, multiKeyslotFile);
		
		// Verify masterkey file exists
		Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
		if (!Files.exists(masterkeyPath)) {
			throw new IOException("No masterkey file found at " + vaultPath);
		}
		
		return manager;
	}

	/**
	 * Save is NO-OP for stealth mode - we don't save metadata.
	 */
	public void save() throws IOException {
		// NO-OP: We don't save identity metadata for plausible deniability
		LOG.debug("Save called but skipped (stealth mode)");
	}

	/**
	 * Get identities - returns empty list for secure design.
	 * 
	 * SECURITY: Cannot enumerate identities without violating plausible deniability.
	 * Unlock process discovers the identity by password match.
	 */
	public List<VaultIdentity> getIdentities() {
		// Return empty list - identities are not enumerable for security reasons
		return Collections.emptyList();
	}

	/**
	 * Get primary identity - returns empty for secure design.
	 * 
	 * SECURITY: Identities are discovered during unlock, not pre-enumerated.
	 */
	public Optional<VaultIdentity> getPrimaryIdentity() {
		return Optional.empty();
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
	 * Delete a keyslot from the vault file.
	 * WARNING: Requires the password of the keyslot to remove it.
	 * NOTE: This method only removes the keyslot. The corresponding config slot
	 * must be removed separately by the caller using MultiKeyslotVaultConfig.
	 * 
	 * @param password Password of the keyslot to remove
	 * @return true if a keyslot was removed
	 */
	public boolean removeKeyslot(CharSequence password) {
		try {
			Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
			
			boolean keyslotRemoved = multiKeyslotFile.removeKeyslot(masterkeyPath, password);
			
			if (keyslotRemoved) {
				LOG.debug("Removed keyslot from vault");
			}
			return keyslotRemoved;
		} catch (IOException e) {
			LOG.error("Failed to remove keyslot", e);
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
