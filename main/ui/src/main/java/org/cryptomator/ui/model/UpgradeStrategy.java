package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import javax.inject.Provider;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.cryptomator.filesystem.crypto.Constants;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeStrategy.class);

	protected final Provider<Cryptor> cryptorProvider;
	protected final Localization localization;

	UpgradeStrategy(Provider<Cryptor> cryptorProvider, Localization localization) {
		this.cryptorProvider = cryptorProvider;
		this.localization = localization;
	}

	/**
	 * @return Localized string to display to the user when an upgrade is needed.
	 */
	public abstract String getNotification(Vault vault);

	/**
	 * Upgrades a vault. Might take a moment, should be run in a background thread.
	 */
	public void upgrade(Vault vault, CharSequence passphrase) throws UpgradeFailedException {
		final Cryptor cryptor = cryptorProvider.get();
		try {
			final Path masterkeyFile = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME);
			final byte[] masterkeyFileContents = Files.readAllBytes(masterkeyFile);
			cryptor.readKeysFromMasterkeyFile(masterkeyFileContents, passphrase);
			// create backup, as soon as we know the password was correct:
			final Path masterkeyBackupFile = vault.path().getValue().resolve(Constants.MASTERKEY_BACKUP_FILENAME);
			Files.copy(masterkeyFile, masterkeyBackupFile, StandardCopyOption.REPLACE_EXISTING);
			// do stuff:
			upgrade(vault, cryptor);
			// write updated masterkey file:
			final byte[] upgradedMasterkeyFileContents = cryptor.writeKeysToMasterkeyFile(passphrase);
			final Path masterkeyFileAfterUpgrading = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME); // path may have changed
			Files.write(masterkeyFileAfterUpgrading, upgradedMasterkeyFileContents, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (InvalidPassphraseException e) {
			throw new UpgradeFailedException(localization.getString("unlock.errorMessage.wrongPassword"));
		} catch (IOException | UnsupportedVaultFormatException e) {
			LOG.warn("Upgrade failed.", e);
			throw new UpgradeFailedException("Upgrade failed. Details in log message.");
		} finally {
			cryptor.destroy();
		}
	}

	protected abstract void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException;

	/**
	 * Determines in O(1), if an upgrade can be applied to a vault.
	 * 
	 * @return <code>true</code> if and only if the vault can be migrated to a newer version without the risk of data losses.
	 */
	public abstract boolean isApplicable(Vault vault);

	/**
	 * Thrown when data migration failed.
	 */
	public class UpgradeFailedException extends Exception {

		UpgradeFailedException() {
		}

		UpgradeFailedException(String message) {
			super(message);
		}

	}

}
