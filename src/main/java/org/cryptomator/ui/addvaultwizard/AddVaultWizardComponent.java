/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultComponent;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import static org.cryptomator.ui.addvaultwizard.ChooseExistingVaultController.prepareVault;

@AddVaultWizardScoped
@Subcomponent(modules = {AddVaultModule.class})
public interface AddVaultWizardComponent {

	@AddVaultWizardWindow
	Stage window();

	@FxmlScene(FxmlFile.ADDVAULT_NEW_NAME)
	Lazy<Scene> sceneNew();

	@FxmlScene(FxmlFile.ADDVAULT_EXISTING)
	Lazy<Scene> sceneExisting();

	@FxmlScene(FxmlFile.ADDVAULT_SUCCESS)
	Lazy<Scene> sceneSuccess();

	default void showAddNewVaultWizard(ResourceBundle resourceBundle) {
		Stage stage = window();
		stage.setScene(sceneNew().get());
		stage.setTitle(resourceBundle.getString("addvaultwizard.new.title"));
		stage.sizeToScene();
		stage.show();
	}

	default void showAddExistingVaultWizard(ResourceBundle resourceBundle) {
		Stage stage = window();
		stage.setScene(sceneExisting().get());
		stage.setTitle(resourceBundle.getString("addvaultwizard.existing.title"));
		stage.sizeToScene();
		stage.show();
	}

	default void showRecoverExistingVaultWizard(Window mainWindow, //
												Dialogs dialogs, //
												VaultComponent.Factory vaultComponentFactory, //
												List<MountService> mountServices, //
												VaultListManager vaultListManager, //
												RecoveryKeyComponent.Factory recoveryKeyWindow) {
		Stage stage = window();
		DirectoryChooser directoryChooser = new DirectoryChooser();

		while (true) {
			File selectedDirectory = directoryChooser.showDialog(mainWindow);
			if (selectedDirectory == null) {
				return;
			}

			boolean hasSubfolderD = new File(selectedDirectory, "d").isDirectory();
			if (!hasSubfolderD) {
				dialogs.prepareNoDDirectorySelectedDialog(stage).build().showAndWait();
				continue;
			}

			Vault preparedVault = prepareVault(selectedDirectory, vaultComponentFactory, mountServices);

			if (!vaultListManager.containsVault(preparedVault.getPath())) {
				vaultListManager.addVault(preparedVault);
				dialogs.prepareRecoveryVaultAdded(stage).setOkAction(Stage::close).build().showAndWait();
			}

			VaultListManager.redetermineVaultState(preparedVault);
			VaultState.Value state = preparedVault.getState();

			switch (state) {
				case VAULT_CONFIG_MISSING -> recoveryKeyWindow.create(preparedVault, stage, new SimpleObjectProperty<>(RecoveryActionType.RESTORE_VAULT_CONFIG)) //
						.showOnboardingDialogWindow();
				case ALL_MISSING -> recoveryKeyWindow.create(preparedVault, stage, new SimpleObjectProperty<>(RecoveryActionType.RESTORE_ALL)) //
						.showOnboardingDialogWindow();
				default -> dialogs.prepareRecoveryVaultAlreadyExists(stage)//
						.setOkAction(Stage::close)//
						.build().showAndWait();
			}
			break;
		}
	}

	@Subcomponent.Builder
	interface Builder {

		AddVaultWizardComponent build();
	}

}
