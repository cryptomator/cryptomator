package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.model.Vault;

import javax.inject.Inject;

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {
	
	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();

	@Inject
	VaultListCellController() {}

	/* Getter/Setter */

	public ObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public void setVault(Vault value) {
		vault.set(value);
	}
}
