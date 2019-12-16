package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final VaultService vaultService;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, VaultService vaultService) {
		this.vault = vault;
		this.vaultService = vaultService;
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

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

}
