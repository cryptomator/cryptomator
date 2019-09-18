package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailUnlockedController.class);

	private final ReadOnlyObjectProperty<Vault> vault;
	private final ExecutorService executor;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, ExecutorService executor) {
		this.vault = vault;
		this.executor = executor;
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
		Vault v = vault.get();
		v.setState(VaultState.PROCESSING);
		Tasks.create(() -> {
			v.lock(false);
		}).onSuccess(() -> {
			LOG.trace("Regular unmount succeeded.");
			v.setState(VaultState.LOCKED);
		}).onError(Exception.class, e -> {
			v.setState(VaultState.UNLOCKED);
			LOG.error("Regular unmount failed.", e);
			// TODO
		}).runOnce(executor);
	}

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

}
