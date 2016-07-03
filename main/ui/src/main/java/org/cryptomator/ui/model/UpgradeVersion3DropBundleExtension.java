package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.cryptomator.filesystem.crypto.Constants;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

@Singleton
class UpgradeVersion3DropBundleExtension extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion3DropBundleExtension.class);
	private final Settings settings;

	@Inject
	public UpgradeVersion3DropBundleExtension(Provider<Cryptor> cryptorProvider, Localization localization, Settings settings) {
		super(cryptorProvider, localization);
		this.settings = settings;
	}

	@Override
	public String getNotification(Vault vault) {
		String fmt = localization.getString("upgrade.version3dropBundleExtension.msg");
		Path path = vault.path().getValue();
		String oldVaultName = path.getFileName().toString();
		String newVaultName = StringUtils.removeEnd(oldVaultName, Vault.VAULT_FILE_EXTENSION);
		return String.format(fmt, oldVaultName, newVaultName);
	}

	@Override
	public void upgrade(Vault vault, CharSequence passphrase) throws UpgradeFailedException {
		final Cryptor cryptor = cryptorProvider.get();
		try {
			final Path masterkeyFile = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME);
			final byte[] masterkeyFileContents = Files.readAllBytes(masterkeyFile);
			cryptor.readKeysFromMasterkeyFile(masterkeyFileContents, passphrase);
			upgrade(vault, cryptor);
			// don't write new masterkey. this is a special case, as we were stupid and didn't increase the vault version with this upgrade...
		} catch (InvalidPassphraseException e) {
			throw new UpgradeFailedException(localization.getString("unlock.errorMessage.wrongPassword"));
		} catch (IOException | UnsupportedVaultFormatException e) {
			LOG.warn("Upgrade failed.", e);
			throw new UpgradeFailedException("Upgrade failed. Details in log message.");
		} finally {
			cryptor.destroy();
		}
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		Path path = vault.path().getValue();
		String oldVaultName = path.getFileName().toString();
		String newVaultName = StringUtils.removeEnd(oldVaultName, Vault.VAULT_FILE_EXTENSION);
		Path newPath = path.resolveSibling(newVaultName);
		if (Files.exists(newPath)) {
			String fmt = localization.getString("upgrade.version3dropBundleExtension.err.alreadyExists");
			String msg = String.format(fmt, newPath);
			throw new UpgradeFailedException(msg);
		} else {
			try {
				Files.move(path, path.resolveSibling(newVaultName));
				Platform.runLater(() -> {
					vault.setPath(newPath);
					settings.save();
				});
			} catch (IOException e) {
				LOG.error("Vault migration failed", e);
				throw new UpgradeFailedException();
			}
		}
	}

	@Override
	public boolean isApplicable(Vault vault) {
		return vault.path().getValue().getFileName().toString().endsWith(Vault.VAULT_FILE_EXTENSION);
	}

}
