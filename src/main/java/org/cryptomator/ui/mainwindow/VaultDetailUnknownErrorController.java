package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private final ObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final Stage errorWindow;
	private final ObservableList<Vault> vaults;
	private final Stage mainWindow;
	private final Dialogs dialogs;

	@Inject
	public VaultDetailUnknownErrorController(@MainWindow Stage mainWindow, //
											 ObjectProperty<Vault> vault, //
											 ObservableList<Vault> vaults, //
											 FxApplicationWindows appWindows, //
											 @Named("errorWindow") Stage errorWindow, //
											 Provider<Dialogs> dialogsProvider) {
		this.mainWindow = mainWindow;
		this.vault = vault;
		this.vaults = vaults;
		this.appWindows = appWindows;
		this.errorWindow = errorWindow;
		this.dialogs = dialogsProvider.get();
	}

	@FXML
	public void showError() {
		appWindows.showErrorWindow(vault.get().getLastKnownException(), errorWindow, null);
	}

	@FXML
	public void reload() {
		VaultListManager.redetermineVaultState(vault.get());
	}

	@FXML
	void didClickRemoveVault() {
		dialogs.prepareRemoveVaultDialog(mainWindow, vault.get(), vaults).build().showAndWait();
	}
}
