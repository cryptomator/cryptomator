package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.common.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailUnlockedController.class);

	private final ReadOnlyObjectProperty<Vault> vault;
	private final VaultService vaultService;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, VaultService vaultService) {
		this.vault = vault;
		this.vaultService = vaultService;
	}

	@FXML
	public void revealAccessLocation() {
		try {
			vault.get().reveal();
		} catch (Volume.VolumeException e) {
			LOG.error("Failed to reveal vault.", e);
		}
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
