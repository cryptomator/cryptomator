package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.Cryptor;
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
	public UpgradeVersion3DropBundleExtension(SecureRandom secureRandom, Localization localization, Settings settings) {
		super(Cryptors.version1(secureRandom), localization, 3, 3);
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
				LOG.info("Renaming {} to {}", path, newPath.getFileName());
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
		Path vaultPath = vault.path().getValue();
		if (vaultPath.toString().endsWith(Vault.VAULT_FILE_EXTENSION)) {
			return super.isApplicable(vault);
		} else {
			return false;
		}
	}

}
