package org.cryptomator.ui.removevault;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@RemoveVaultScoped
public class RemoveVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RemoveVaultController.class);

	private final Stage window;
	private final Vault vault;
	private final ObservableList<Vault> vaults;

	@Inject
	public RemoveVaultController(@RemoveVaultWindow Stage window, @RemoveVaultWindow Vault vault, ObservableList<Vault> vaults) {
		this.window = window;
		this.vault = vault;
		this.vaults = vaults;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void finish() {
		vaults.remove(vault);
		LOG.debug("Removing vault {}.", vault.getDisplayName());
		window.close();
	}
}
