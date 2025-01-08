package org.cryptomator.ui.dialogs;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;
import javafx.stage.Stage;


public class Dialogs {

	private static final Logger LOG = LoggerFactory.getLogger(Dialogs.class);

	private Dialogs() {}

	public static void showRemoveVaultDialog(SimpleDialog.Builder simpleDialogBuilder, Stage mainWindow, Vault vault, ObservableList<Vault> vaults) {
		simpleDialogBuilder.setOwner(mainWindow) //
				.setTitleKey("removeVault.title", vault.getDisplayName()) //
				.setMessageKey("removeVault.message") //
				.setDescriptionKey("removeVault.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("removeVault.confirmBtn") //
				.setCancelButtonKey("generic.button.cancel") //
				.setOkAction(v -> {
					LOG.debug("Removing vault {}.", vault.getDisplayName());
					vaults.remove(vault);
					v.close();
				}) //
				.build().showAndWait();
	}
}
