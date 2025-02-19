package org.cryptomator.ui.dialogs;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@FxApplicationScoped
public class Dialogs {

	private final ResourceBundle resourceBundle;
	private final StageFactory stageFactory;

	@Inject
	public Dialogs(ResourceBundle resourceBundle, StageFactory stageFactory) {
		this.resourceBundle = resourceBundle;
		this.stageFactory = stageFactory;
	}

	private static final Logger LOG = LoggerFactory.getLogger(Dialogs.class);

	private SimpleDialog.Builder createDialogBuilder() {
		return new SimpleDialog.Builder(resourceBundle, stageFactory);
	}

	public SimpleDialog.Builder prepareRemoveVaultDialog(Stage window, Vault vault, ObservableList<Vault> vaults) {
		return createDialogBuilder().setOwner(window) //
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
				});
	}

	public SimpleDialog.Builder prepareRemoveCertDialog(Stage window, Settings settings) {
		return createDialogBuilder() //
				.setOwner(window) //
				.setTitleKey("removeCert.title") //
				.setMessageKey("removeCert.message") //
				.setDescriptionKey("removeCert.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("removeCert.confirmBtn") //
				.setCancelButtonKey("generic.button.cancel") //
				.setOkAction(stage -> {
					settings.licenseKey.set(null);
					stage.close();
				});
	}

	public SimpleDialog.Builder prepareDokanySupportEndDialog(Stage window, Consumer<Stage> cancelAction) {
		return createDialogBuilder() //
				.setOwner(window) //
				.setTitleKey("dokanySupportEnd.title") //
				.setMessageKey("dokanySupportEnd.message") //
				.setDescriptionKey("dokanySupportEnd.description") //
				.setIcon(FontAwesome5Icon.EXCLAMATION) //
				.setOkButtonKey("generic.button.close") //
				.setCancelButtonKey("dokanySupportEnd.preferencesBtn") //
				.setOkAction(Stage::close) //
				.setCancelAction(cancelAction);
	}

	public SimpleDialog.Builder prepareRetryIfReadonlyDialog(Stage window, Consumer<Stage> okAction) {
		return createDialogBuilder() //
				.setOwner(window) //
				.setTitleKey("retryIfReadonly.title") //
				.setMessageKey("retryIfReadonly.message") //
				.setDescriptionKey("retryIfReadonly.description") //
				.setIcon(FontAwesome5Icon.EXCLAMATION) //
				.setOkButtonKey("retryIfReadonly.retry") //
				.setCancelButtonKey("generic.button.close") //
				.setOkAction(okAction) //
				.setCancelAction(Stage::close);
	}
}
