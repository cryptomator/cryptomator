package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.fxapp.FxApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@MainWindowScoped
public class VaultDetailController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailController.class);

	private final ReadOnlyObjectProperty<Vault> vault;
	private final ExecutorService executor;
	private final FxApplication application;

	@Inject
	VaultDetailController(ObjectProperty<Vault> vault, ExecutorService executor, FxApplication application) {
		this.vault = vault;
		this.executor = executor;
		this.application = application;
	}

	@FXML
	public void unlock() {
		application.showUnlockWindow(vault.get());
	}

	@FXML
	public void lock() {
		vault.get().setState(Vault.State.PROCESSING);
		Tasks.create(() -> {
			vault.get().lock(false);
		}).onSuccess(() -> {
			LOG.trace("Regular unmount succeeded.");
			vault.get().setState(Vault.State.LOCKED);
		}).onError(Exception.class, e -> {
			vault.get().setState(Vault.State.UNLOCKED);
			// TODO
		}).runOnce(executor);
	}

	/* Observable Properties */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}
	
}
