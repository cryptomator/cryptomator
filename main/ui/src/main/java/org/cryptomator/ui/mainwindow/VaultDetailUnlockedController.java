package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.stats.VaultStatisticsComponent;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final VaultService vaultService;
	private final VaultStatisticsComponent.Builder vaultStatisticsWindow;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, VaultService vaultService, VaultStatisticsComponent.Builder vaultStatisticsWindow) {
		this.vault = vault;
		this.vaultService = vaultService;
		this.vaultStatisticsWindow = vaultStatisticsWindow;
	}

	@FXML
	public void revealAccessLocation() {
		vaultService.reveal(vault.get());
	}

	@FXML
	public void lock() {
		vaultService.lock(vault.get(), false);
		// TODO count lock attempts, and allow forced lock
	}

	@FXML
	public void showVaultStatistics() {
		vaultStatisticsWindow.vault(vault.get()).build().showVaultStatisticsWindow();
	}

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

}
