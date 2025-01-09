package org.cryptomator.ui.dialogs;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;
import javafx.stage.Stage;
import java.util.function.Consumer;


public class Dialogs {

	private static final Logger LOG = LoggerFactory.getLogger(Dialogs.class);

	public static SimpleDialog buildRemoveVaultDialog(SimpleDialog.Builder simpleDialogBuilder, Stage window, Vault vault, ObservableList<Vault> vaults) {
		return simpleDialogBuilder.setOwner(window) //
				.setTitleKey("removeVault.title", vault.getDisplayName()) //
				.setMessageKey("removeVault.message") //
				.setDescriptionKey("removeVault.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("removeVault.confirmBtn") //
				.setCancelButtonKey("generic.button.cancel") //
				.setOkAction(stage -> {
					LOG.debug("Removing vault {}.", vault.getDisplayName());
					vaults.remove(vault);
					stage.close();
				}) //
				.build();
	}

	public static SimpleDialog buildRemoveCertDialog(SimpleDialog.Builder simpleDialogBuilder, Stage window, Settings settings) {
		return simpleDialogBuilder.setOwner(window) //
				.setTitleKey("removeCert.title") //
				.setMessageKey("removeCert.message") //
				.setDescriptionKey("removeCert.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("removeCert.confirmBtn") //
				.setCancelButtonKey("generic.button.cancel").setOkAction(v -> {
					settings.licenseKey.set(null);
					v.close();
				}) //
				.setCancelAction(Stage::close) //
				.build();
	}

	public static SimpleDialog buildDokanySupportEndDialog(SimpleDialog.Builder simpleDialogBuilder, Stage window, Consumer<Stage> cancelAction) {
		return simpleDialogBuilder.setOwner(window) //
				.setTitleKey("dokanySupportEnd.title") //
				.setMessageKey("dokanySupportEnd.message") //
				.setDescriptionKey("dokanySupportEnd.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("generic.button.close") //
				.setCancelButtonKey("dokanySupportEnd.preferencesBtn") //
				.setOkAction(Stage::close) //
				.setCancelAction(cancelAction) //
				.build();
	}

}
