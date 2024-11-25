package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.CustomDialog;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailUnknownErrorController.class);

	private final ObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final Stage errorWindow;
	private final ObservableList<Vault> vaults;
	private final Stage mainWindow;
	private final Provider<CustomDialog.Builder> customDialogProvider;

	@Inject
	public VaultDetailUnknownErrorController(@MainWindow Stage mainWindow, //
											 ObjectProperty<Vault> vault, //
											 ObservableList<Vault> vaults, //
											 FxApplicationWindows appWindows, //
											 @Named("errorWindow") Stage errorWindow, //
											 Provider<CustomDialog.Builder> customDialogProvider) {
		this.mainWindow = mainWindow;
		this.vault = vault;
		this.vaults = vaults;
		this.appWindows = appWindows;
		this.errorWindow = errorWindow;
		this.customDialogProvider = customDialogProvider;
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
		customDialogProvider.get().setOwner(mainWindow) //
				.setTitleKey("removeVault.title", vault.get().getDisplayName()) //
				.setMessageKey("removeVault.message") //
				.setDescriptionKey("removeVault.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("removeVault.confirmBtn") //
				.setCancelButtonKey("generic.button.cancel") //
				.setOkAction(v -> {
					LOG.debug("Removing vault {}.", vault.get().getDisplayName());
					vaults.remove(vault.get());
					v.close();
				}) //
				.setCancelAction(Stage::close) //
				.build().showAndWait();
	}
}
