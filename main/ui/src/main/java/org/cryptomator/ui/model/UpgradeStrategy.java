package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.KeyFile;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.filesystem.crypto.Constants;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeStrategy.class);

	protected final CryptorProvider cryptorProvider;
	protected final Localization localization;
	protected final int vaultVersionBeforeUpgrade;
	protected final int vaultVersionAfterUpgrade;

	UpgradeStrategy(CryptorProvider cryptorProvider, Localization localization, int vaultVersionBeforeUpgrade, int vaultVersionAfterUpgrade) {
		this.cryptorProvider = cryptorProvider;
		this.localization = localization;
		this.vaultVersionBeforeUpgrade = vaultVersionBeforeUpgrade;
		this.vaultVersionAfterUpgrade = vaultVersionAfterUpgrade;
	}

	/**
	 * @return Localized title string to display to the user when an upgrade is needed.
	 */
	public abstract String getTitle(Vault vault);

	/**
	 * @return Localized message string to display to the user when an upgrade is needed.
	 */
	public abstract String getMessage(Vault vault);

	/**
	 * Upgrades a vault. Might take a moment, should be run in a background thread.
	 */
	public void upgrade(Vault vault, CharSequence passphrase) throws UpgradeFailedException {
		LOG.info("Upgrading {} from {} to {}.", vault.path().getValue(), vaultVersionBeforeUpgrade, vaultVersionAfterUpgrade);
		Cryptor cryptor = null;
		try {
			final Path masterkeyFile = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME);
			final byte[] masterkeyFileContents = Files.readAllBytes(masterkeyFile);
			cryptor = cryptorProvider.createFromKeyFile(KeyFile.parse(masterkeyFileContents), passphrase, vaultVersionBeforeUpgrade);
			// create backup, as soon as we know the password was correct:
			final Path masterkeyBackupFile = vault.path().getValue().resolve(Constants.MASTERKEY_BACKUP_FILENAME);
			Files.copy(masterkeyFile, masterkeyBackupFile, StandardCopyOption.REPLACE_EXISTING);
			LOG.info("Backuped masterkey.");
			// do stuff:
			upgrade(vault, cryptor);
			// write updated masterkey file:
			final byte[] upgradedMasterkeyFileContents = cryptor.writeKeysToMasterkeyFile(passphrase, vaultVersionAfterUpgrade).serialize();
			final Path masterkeyFileAfterUpgrade = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME); // path may have changed
			Files.write(masterkeyFileAfterUpgrade, upgradedMasterkeyFileContents, StandardOpenOption.TRUNCATE_EXISTING);
			LOG.info("Updated masterkey.");
		} catch (InvalidPassphraseException e) {
			throw new UpgradeFailedException(localization.getString("unlock.errorMessage.wrongPassword"));
		} catch (UnsupportedVaultFormatException e) {
			if (e.getDetectedVersion() == Integer.MAX_VALUE) {
				LOG.warn("Version MAC authentication error in vault {}", vault.path().get());
				throw new UpgradeFailedException(localization.getString("unlock.errorMessage.unauthenticVersionMac"));
			} else {
				LOG.warn("Upgrade failed.", e);
				throw new UpgradeFailedException("Upgrade failed. Details in log message.");
			}
		} catch (IOException e) {
			LOG.warn("Upgrade failed.", e);
			throw new UpgradeFailedException("Upgrade failed. Details in log message.");
		} finally {
			if (cryptor != null) {
				cryptor.destroy();
			}
		}
	}

	protected abstract void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException;

	/**
	 * Determines in O(1), if an upgrade can be applied to a vault.
	 * 
	 * @return <code>true</code> if and only if the vault can be migrated to a newer version without the risk of data losses.
	 */
	public boolean isApplicable(Vault vault) {
		final Path masterkeyFile = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME);
		try {
			if (Files.isRegularFile(masterkeyFile)) {
				byte[] masterkeyFileContents = Files.readAllBytes(masterkeyFile);
				return KeyFile.parse(masterkeyFileContents).getVersion() == vaultVersionBeforeUpgrade;
			} else {
				LOG.warn("Not a file: {}", masterkeyFile);
				return false;
			}
		} catch (IOException e) {
			LOG.warn("Could not determine, whether upgrade is applicable.", e);
			return false;
		}
	}

	/**
	 * Thrown when data migration failed.
	 */
	public static class UpgradeFailedException extends Exception {

		UpgradeFailedException() {
		}

		UpgradeFailedException(String message) {
			super(message);
		}

	}

}
