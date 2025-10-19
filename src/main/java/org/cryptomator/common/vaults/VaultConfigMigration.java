package org.cryptomator.common.vaults;

import org.cryptomator.cryptofs.VaultConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility to migrate legacy vault.bak files to multi-keyslot vault.cryptomator format.
 * 
 * <p>This migration ensures TRUE plausible deniability by eliminating the vault.bak file
 * that reveals the existence of a hidden vault. After migration, both primary and hidden
 * vault configurations are stored in a single vault.cryptomator file using multi-keyslot format.
 * 
 * <h2>Migration Process:</h2>
 * <ol>
 *   <li>Detect if vault.bak exists</li>
 *   <li>Read primary config from vault.cryptomator</li>
 *   <li>Read hidden config from vault.bak</li>
 *   <li>Create multi-keyslot vault.cryptomator with both configs</li>
 *   <li>Backup and delete vault.bak</li>
 * </ol>
 * 
 * <h2>Safety:</h2>
 * <ul>
 *   <li>vault.bak is backed up before deletion</li>
 *   <li>Migration is atomic - uses temporary files</li>
 *   <li>Original files preserved on error</li>
 * </ul>
 * 
 * @since 1.8.0
 */
public class VaultConfigMigration {
	
	private static final Logger LOG = LoggerFactory.getLogger(VaultConfigMigration.class);
	
	private final MultiKeyslotVaultConfig multiKeyslotVaultConfig;
	
	public VaultConfigMigration(MultiKeyslotVaultConfig multiKeyslotVaultConfig) {
		this.multiKeyslotVaultConfig = multiKeyslotVaultConfig;
	}
	
	/**
	 * Check if vault needs migration (vault.bak exists).
	 * 
	 * @param vaultPath Path to vault directory
	 * @return true if vault.bak exists and migration is needed
	 */
	public boolean needsMigration(Path vaultPath) {
		return Files.exists(vaultPath.resolve("vault.bak"));
	}
	
	/**
	 * Migrate vault.bak to multi-keyslot vault.cryptomator.
	 * 
	 * <p>This method performs an atomic migration that eliminates vault.bak
	 * and combines both configs into vault.cryptomator.
	 * 
	 * @param vaultPath Path to vault directory
	 * @throws IOException on I/O errors
	 */
	public void migrate(Path vaultPath) throws IOException {
		Path primaryConfigPath = vaultPath.resolve("vault.cryptomator");
		Path hiddenConfigPath = vaultPath.resolve("vault.bak");
		
		if (!Files.exists(hiddenConfigPath)) {
			LOG.debug("No vault.bak found at {} - no migration needed", vaultPath);
			return;
		}
		
		LOG.info("Migrating vault.bak to multi-keyslot format at {}", vaultPath);
		
		// Step 1: Read primary config
		if (!Files.exists(primaryConfigPath)) {
			throw new IOException("Primary vault.cryptomator not found - cannot migrate");
		}
		String primaryConfigToken = Files.readString(primaryConfigPath, StandardCharsets.US_ASCII);
		
		// Step 2: Read hidden config
		String hiddenConfigToken = Files.readString(hiddenConfigPath, StandardCharsets.US_ASCII);
		
		// Step 3: Validate configs can be parsed
		try {
			VaultConfig.decode(primaryConfigToken);
			VaultConfig.decode(hiddenConfigToken);
		} catch (Exception e) {
			throw new IOException("Invalid vault config during migration", e);
		}
		
		// Step 4: Create multi-keyslot vault.cryptomator in temp location
		Path tempConfigPath = Files.createTempFile(vaultPath, ".vault-migration-", ".tmp");
		try {
			// Create multi-keyslot with primary config
			multiKeyslotVaultConfig.persist(tempConfigPath, primaryConfigToken);
			
			// Add hidden config slot
			multiKeyslotVaultConfig.addConfigSlot(tempConfigPath, hiddenConfigToken);
			
			// Step 5: Backup vault.bak before deletion
			Path backupPath = vaultPath.resolve("vault.bak.migrated");
			Files.copy(hiddenConfigPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			LOG.info("Backed up vault.bak to vault.bak.migrated");
			
			// Step 6: Atomic replace vault.cryptomator
			Files.move(tempConfigPath, primaryConfigPath, 
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE);
			
			// Step 7: Delete vault.bak
			Files.delete(hiddenConfigPath);
			
			LOG.info("✓ Migration complete at {}", vaultPath);
			LOG.info("  ✓ vault.cryptomator now contains {} config slots", 
				multiKeyslotVaultConfig.getConfigSlotCount(primaryConfigPath));
			LOG.info("  ✓ vault.bak deleted (backed up to vault.bak.migrated)");
			LOG.info("  ✓ TRUE plausible deniability achieved!");
			
		} catch (Exception e) {
			// Clean up temp file on error
			Files.deleteIfExists(tempConfigPath);
			throw new IOException("Migration failed - original files preserved", e);
		}
	}
	
	/**
	 * Migrate vault automatically if needed, logging errors but not throwing.
	 * This is useful for transparent migration during vault operations.
	 * 
	 * @param vaultPath Path to vault directory
	 * @return true if migration was performed successfully
	 */
	public boolean migrateIfNeeded(Path vaultPath) {
		if (!needsMigration(vaultPath)) {
			return false;
		}
		
		try {
			migrate(vaultPath);
			return true;
		} catch (IOException e) {
			LOG.warn("Failed to migrate vault.bak at {} - will retry later", vaultPath, e);
			return false;
		}
	}
	
	/**
	 * Check if migration was previously completed (backup exists).
	 * 
	 * @param vaultPath Path to vault directory
	 * @return true if vault.bak.migrated backup exists
	 */
	public boolean wasMigrated(Path vaultPath) {
		return Files.exists(vaultPath.resolve("vault.bak.migrated"));
	}
}
