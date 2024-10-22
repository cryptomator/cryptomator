package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.CustomDialogBuilder;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailUnknownErrorController.class);

	private final ObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final Stage errorWindow;
	private final ObservableList<Vault> vaults;
	private final ResourceBundle resourceBundle;
	private final Stage mainWindow;

	@Inject
	public VaultDetailUnknownErrorController(@MainWindow Stage mainWindow,
											 ObjectProperty<Vault> vault, ObservableList<Vault> vaults, //
											 ResourceBundle resourceBundle, //
											 FxApplicationWindows appWindows, @Named("errorWindow") Stage errorWindow) {
		this.mainWindow = mainWindow;
		this.vault = vault;
		this.vaults = vaults;
		this.resourceBundle = resourceBundle;
		this.appWindows = appWindows;
		this.errorWindow = errorWindow;
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
		new CustomDialogBuilder() //
				.setTitle(String.format(resourceBundle.getString("removeVault.title"), vault.get().getDisplayName())) //
				.setMessage(resourceBundle.getString("removeVault.message")) //
				.setDescription(resourceBundle.getString("removeVault.description")) //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonText(resourceBundle.getString("removeVault.confirmBtn")) //
				.setCancelButtonText(resourceBundle.getString("generic.button.cancel")) //
				.setOkAction(v -> {
					LOG.debug("Removing vault {}.", vault.get().getDisplayName());
					vaults.remove(vault.get());
					v.close();
				}) //
				.setCancelAction(Stage::close) //
				.buildAndShow(mainWindow);
	}
}
