package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailMissingVaultController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;

	@Inject
	public VaultDetailMissingVaultController(ObjectProperty<Vault> vault) {
		this.vault = vault;
	}

}
