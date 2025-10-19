package org.cryptomator.common.vaults;

import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
	 * Delete a keyslot AND its corresponding config slot from the vault.
	 * This ensures complete removal of hidden identities for plausible deniability.
	 * 
	 * SECURITY: Requires the password of the identity to remove.
	 * Both masterkey keyslot and vault config slot must be removed to prevent
	 * orphaned data that could reveal hidden vault existence.
	 * 
	 * @param password Password of the identity to remove
	 * @return true if the identity was successfully removed
	 */
	public boolean removeKeyslot(CharSequence password) {
		Masterkey hiddenMasterkey = null;
		byte[] masterkeyBytes = null;
		
		try {
			Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
			Path vaultConfigPath = vaultPath.resolve("vault.cryptomator");
			
			// Step 1: Load the masterkey using the password
			// This is needed to identify which config slot to remove
			try {
				hiddenMasterkey = multiKeyslotFile.load(masterkeyPath, password);
				masterkeyBytes = hiddenMasterkey.getEncoded();
			} catch (InvalidPassphraseException e) {
				LOG.warn("Password doesn't match any identity");
				return false;
			}
			
			// Step 2: Remove config slot from vault.cryptomator
			MultiKeyslotVaultConfig configHandler = new MultiKeyslotVaultConfig();
			boolean configRemoved = configHandler.removeConfigSlot(vaultConfigPath, masterkeyBytes);
			if (!configRemoved) {
				LOG.warn("Failed to remove config slot - may be single-config format");
				// Continue anyway to remove keyslot
			}
			
			// Step 3: Remove keyslot from masterkey.cryptomator
			boolean keyslotRemoved = multiKeyslotFile.removeKeyslot(masterkeyPath, password);
			
			if (keyslotRemoved) {
				LOG.debug("Removed identity from vault (keyslot and config)");
			}
			return keyslotRemoved;
			
		} catch (IOException e) {
			LOG.error("Failed to remove identity", e);
			return false;
		} finally {
			// SECURITY: Always clean up sensitive masterkey data
			if (hiddenMasterkey != null) {
				hiddenMasterkey.destroy();
			}
			if (masterkeyBytes != null) {
				Arrays.fill(masterkeyBytes, (byte) 0);
			}
		}
	}
	
	/**
	 * Get the multi-keyslot file handler.
	 */
	public MultiKeyslotFile getMultiKeyslotFile() {
		return multiKeyslotFile;
	}
}
