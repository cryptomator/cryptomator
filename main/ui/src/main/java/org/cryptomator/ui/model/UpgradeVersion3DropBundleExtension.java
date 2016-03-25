package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

class UpgradeVersion3DropBundleExtension implements UpgradeInstruction {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion3DropBundleExtension.class);

	@Override
	public String getNotification(Vault vault, Localization localization) {
		String fmt = localization.getString("upgrade.version3dropBundleExtension.msg");
		Path path = vault.path().getValue();
		String oldVaultName = path.getFileName().toString();
		String newVaultName = StringUtils.removeEnd(oldVaultName, Vault.VAULT_FILE_EXTENSION);
		return String.format(fmt, oldVaultName, newVaultName);
	}

	@Override
	public void upgrade(Vault vault, Localization localization) throws UpgradeFailedException {
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
