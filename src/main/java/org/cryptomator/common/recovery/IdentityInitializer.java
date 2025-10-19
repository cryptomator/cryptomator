package org.cryptomator.common.recovery;

import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.cryptomator.common.vaults.MultiKeyslotVaultConfig;
import org.cryptomator.common.vaults.VaultIdentity;
import org.cryptomator.common.vaults.VaultIdentityManager;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for initializing vault identities during vault creation.
 * TrueCrypt-style: Uses multi-keyslot files for true plausible deniability.
 */
public final class IdentityInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(IdentityInitializer.class);

	private IdentityInitializer() {}

	/**
	 * Initialize a new vault with a primary identity.
	 * Creates a multi-keyslot masterkey file with single keyslot.
	 * TrueCrypt-style: All vaults use multi-keyslot format from the start.
	 */
	public static void initializePrimaryIdentity(Path vaultPath, String identityName, 
												  String identityDescription, Masterkey masterkey, 
												  CharSequence password, 
												  MultiKeyslotFile multiKeyslotFile) throws IOException {
		// Create multi-keyslot file with primary keyslot
		Path masterkeyPath = vaultPath.resolve("masterkey.cryptomator");
		multiKeyslotFile.persist(masterkeyPath, masterkey, password, 9);
		
		LOG.info("Initialized primary vault at {} (multi-keyslot format)", vaultPath);
	}

	/**
	 * Add a hidden vault (plausible deniability).
	 * TrueCrypt-style: Adds keyslot to masterkey.cryptomator AND config slot to vault.cryptomator.
	 * This ensures TRUE plausible deniability - NO vault.bak file reveals hidden vault!
	 * 
	 * @param vaultPath Path to vault
	 * @param identityName Name for hidden vault (not persisted)
	 * @param identityDescription Description (not persisted)
	 * @param primaryPassword Password for primary keyslot (needed for legacy conversion)
	 * @param hiddenPassword Password for hidden keyslot
	 * @param multiKeyslotFile Multi-keyslot file handler
	 * @param multiKeyslotVaultConfig Multi-keyslot vault config handler
	 * @return VaultIdentity for hidden vault
	 * @throws IOException on errors
	 */
	public static VaultIdentity addSecondaryIdentity(Path vaultPath, String identityName, 
													 String identityDescription, CharSequence primaryPassword,
													 CharSequence hiddenPassword, 
													 MultiKeyslotFile multiKeyslotFile,
													 MultiKeyslotVaultConfig multiKeyslotVaultConfig) throws IOException {
		Path masterkeyPath = vaultPath.resolve("masterkey.cryptomator");
		Path vaultConfigPath = vaultPath.resolve("vault.cryptomator");
		
		// SECURITY: Verify primary password before making any changes
		// This prevents unauthorized keyslot addition
		try (Masterkey verifyKey = multiKeyslotFile.load(masterkeyPath, primaryPassword)) {
			LOG.debug("Primary password verified");
			// Password is valid, proceed with hidden vault creation
		} catch (org.cryptomator.cryptolib.api.InvalidPassphraseException e) {
			throw new IOException("Primary password verification failed - access denied", e);
		}
		
		// Generate DIFFERENT masterkey for hidden vault (true plausible deniability)
		Masterkey hiddenMasterkey;
		try {
			hiddenMasterkey = Masterkey.generate(java.security.SecureRandom.getInstanceStrong());
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException("Failed to generate secure random for hidden vault", e);
		}
		
		try {
			// Create hidden identity
			VaultIdentity hiddenIdentity = VaultIdentity.createSecondary(identityName, identityDescription);
			
			// Step 1: Add keyslot to existing masterkey.cryptomator file
			// Pass primaryPassword for legacy conversion (if needed)
			multiKeyslotFile.addKeyslot(masterkeyPath, hiddenMasterkey, hiddenPassword, primaryPassword, 9);
			
		// Step 2: Create hidden vault config in temporary location
		Path tempVaultDir = Files.createTempDirectory("vlt-");
			try {
				MasterkeyLoader loader = ignored -> hiddenMasterkey.copy();
				CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties()
						.withKeyLoader(loader)
						.withVaultConfigFilename("vault.cryptomator")
						.build();
				
				// Initialize hidden vault config in temp location
				CryptoFileSystemProvider.initialize(tempVaultDir, fsProps, URI.create("masterkeyfile:hidden"));
				
				// Step 3: Read the generated hidden vault config
				Path tempConfigPath = tempVaultDir.resolve("vault.cryptomator");
				String hiddenConfigToken = Files.readString(tempConfigPath, StandardCharsets.US_ASCII);
				
				// Step 4: Add hidden config to multi-keyslot vault.cryptomator
				multiKeyslotVaultConfig.addConfigSlot(vaultConfigPath, hiddenConfigToken);
				
				// Step 5: Copy directory structure from temp vault to actual vault
				// The temp vault has the correct root directory structure for this config's JTI
				Path tempDataDir = tempVaultDir.resolve("d");
				Path actualDataDir = vaultPath.resolve("d");
				
				if (Files.exists(tempDataDir)) {
					// Copy the directory tree from temp to actual vault
					Files.walk(tempDataDir)
						.forEach(source -> {
							try {
								Path destination = actualDataDir.resolve(tempDataDir.relativize(source));
								if (Files.isDirectory(source)) {
									Files.createDirectories(destination);
								}
								// Don't copy files, just create directory structure
							} catch (IOException e) {
								LOG.warn("Failed to copy directory structure: {}", e.getMessage());
							}
						});
					LOG.info("Copied hidden vault directory structure from temp location");
				}
				
				LOG.info("Added hidden vault '{}' at {} - TRUE plausible deniability!", identityName, vaultPath);
				LOG.info("  ✓ Keyslot added to masterkey.cryptomator");
				LOG.info("  ✓ Config slot added to vault.cryptomator");
				LOG.info("  ✓ Directory structure initialized");
				LOG.info("  ✓ NO vault.bak created - undetectable!");
				
			} finally {
				// Clean up temp directory recursively
				try {
					// Delete all files in temp directory first
					if (Files.exists(tempVaultDir)) {
						Files.walk(tempVaultDir)
							.sorted(java.util.Comparator.reverseOrder())
							.forEach(path -> {
								try {
									Files.delete(path);
								} catch (IOException e) {
									// Ignore cleanup errors
								}
							});
					}
				} catch (IOException e) {
					// Ignore cleanup errors
				}
			}
			
			return hiddenIdentity;
		} catch (Exception e) {
			throw new IOException("Failed to create hidden vault", e);
		} finally {
			// Always destroy the masterkey after use
			hiddenMasterkey.destroy();
		}
	}

	/**
	 * Remove the hidden vault (secondary identity).
	 * TrueCrypt-style: Removes keyslot from masterkey.cryptomator.
	 * Requires the password of the hidden vault to identify which keyslot to remove.
	 * 
	 * @param vaultPath Path to vault
	 * @param hiddenPassword Password of hidden vault to remove
	 * @param multiKeyslotFile Multi-keyslot file handler
	 * @return true if a keyslot was removed
	 * @throws IOException on errors
	 */
	public static boolean removeIdentity(Path vaultPath, CharSequence hiddenPassword, 
										 MultiKeyslotFile multiKeyslotFile) throws IOException {
		VaultIdentityManager manager = VaultIdentityManager.load(vaultPath, multiKeyslotFile);
		
		// Remove the keyslot that matches the hidden password
		boolean removed = manager.removeKeyslot(hiddenPassword);
		if (removed) {
			LOG.info("Removed keyslot from vault");
		}
		
		return removed;
	}

	/**
	 * Check if a vault has multiple identities (keyslots).
	 * WARNING: This reveals hidden vault existence! Use with caution.
	 */
	public static boolean hasMultipleIdentities(Path vaultPath, MultiKeyslotFile multiKeyslotFile) {
		try {
			VaultIdentityManager manager = VaultIdentityManager.load(vaultPath, multiKeyslotFile);
			return manager.getIdentities().size() > 1;
		} catch (IOException e) {
			LOG.warn("Failed to check for multiple identities", e);
			return false;
		}
	}
}
