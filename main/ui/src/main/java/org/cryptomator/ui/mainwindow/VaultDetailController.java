package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import org.cryptomator.ui.FxApplicationScoped;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@FxApplicationScoped
public class VaultDetailController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailController.class);

	private final ObjectProperty<Vault> vault;

	@Inject
	VaultDetailController(ObjectProperty<Vault> vault) {
		this.vault = vault;
	}

	public ObjectProperty<Vault> vaultProperty() {
		return vault;
	}
	
	public Vault getVault() {
		return vault.get();
	}

}
