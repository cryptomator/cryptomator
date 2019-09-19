package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.migration.MigrationComponent;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailNeedsMigrationController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final MigrationComponent.Builder vaultMigrationWindow;

	@Inject
	public VaultDetailNeedsMigrationController(ObjectProperty<Vault> vault, MigrationComponent.Builder vaultMigrationWindow) {
		this.vault = vault;
		this.vaultMigrationWindow = vaultMigrationWindow;
	}

	@FXML
	public void showVaultMigrator() {
		vaultMigrationWindow.vault(vault.get()).build().showMigrationWindow();
	}
}
