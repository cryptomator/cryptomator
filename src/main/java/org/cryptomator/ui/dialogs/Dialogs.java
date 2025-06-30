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

	private static final String BUTTON_KEY_CLOSE = "generic.button.close";

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
				.setOkButtonKey("generic.button.remove") //
				.setCancelButtonKey("generic.button.cancel") //
				.setOkAction(stage -> {
					LOG.debug("Removing vault {}.", vault.getDisplayName());
					vaults.remove(vault);
					stage.close();
				});
	}

	public SimpleDialog.Builder prepareContactHubAdmin(Stage window) {
		return createDialogBuilder().setOwner(window) //
				.setTitleKey("contactHubAdmin.title") //
				.setMessageKey("contactHubAdmin.message") //
				.setDescriptionKey("contactHubAdmin.description") //
				.setIcon(FontAwesome5Icon.EXCLAMATION)//
				.setOkButtonKey(BUTTON_KEY_CLOSE);
	}

	public SimpleDialog.Builder prepareRecoveryVaultAdded(Stage window) {
		return createDialogBuilder().setOwner(window) //
				.setTitleKey("recoveryKey.recoverExisting.title") //
				.setMessageKey("recoveryKey.recoverExisting.message") //
				.setDescriptionKey("recoveryKey.recoverExisting.description") //
				.setIcon(FontAwesome5Icon.EXCLAMATION)//
				.setOkButtonKey(BUTTON_KEY_CLOSE);
	}
	public SimpleDialog.Builder prepareRecoveryVaultAlreadyExists(Stage window) {
		return createDialogBuilder().setOwner(window) //
				.setTitleKey("recoveryKey.alreadyExists.title") //
				.setMessageKey("recoveryKey.alreadyExists.message") //
				.setDescriptionKey("recoveryKey.alreadyExists.description") //
				.setIcon(FontAwesome5Icon.EXCLAMATION)//
				.setOkButtonKey(BUTTON_KEY_CLOSE);
	}

	public SimpleDialog.Builder prepareRecoverPasswordSuccess(Stage window, Stage owner, ResourceBundle resourceBundle) {
		return createDialogBuilder()
				.setOwner(window) //
				.setTitleKey("recoveryKey.recover.title") //
				.setMessageKey("recoveryKey.recover.resetSuccess.message") //
				.setDescriptionKey("recoveryKey.recover.resetSuccess.description") //
				.setIcon(FontAwesome5Icon.CHECK)
				.setOkAction(stage -> {
					stage.close();
					String ownerTitle = owner.getTitle();
					if (ownerTitle != null && ownerTitle.equals(resourceBundle.getString("addvaultwizard.existing.title"))) {
						owner.close();
					}
				})
				.setOkButtonKey(BUTTON_KEY_CLOSE);
	}

	public SimpleDialog.Builder prepareRemoveCertDialog(Stage window, Settings settings) {
		return createDialogBuilder() //
				.setOwner(window) //
				.setTitleKey("removeCert.title") //
				.setMessageKey("removeCert.message") //
				.setDescriptionKey("removeCert.description") //
				.setIcon(FontAwesome5Icon.QUESTION) //
				.setOkButtonKey("generic.button.remove") //
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
				.setOkButtonKey(BUTTON_KEY_CLOSE) //
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
				.setCancelButtonKey(BUTTON_KEY_CLOSE) //
				.setOkAction(okAction) //
				.setCancelAction(Stage::close);
	}

	public SimpleDialog.Builder prepareNoDDirectorySelectedDialog(Stage window) {
		return createDialogBuilder() //
				.setOwner(window) //
				.setTitleKey("recoveryKey.noDDirDetected.title") //
				.setMessageKey("recoveryKey.noDDirDetected.message") //
				.setDescriptionKey("recoveryKey.noDDirDetected.description") //
				.setIcon(FontAwesome5Icon.EXCLAMATION) //
				.setOkButtonKey("generic.button.change") //
				.setOkAction(Stage::close);
	}

}
