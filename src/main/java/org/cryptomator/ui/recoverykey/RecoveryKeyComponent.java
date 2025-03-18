package org.cryptomator.ui.recoverykey;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.RecoverUtil;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

@RecoveryKeyScoped
@Subcomponent(modules = {RecoveryKeyModule.class})
public interface RecoveryKeyComponent {

	@RecoveryKeyWindow
	Stage window();

	@FxmlScene(FxmlFile.RECOVERYKEY_CREATE)
	Lazy<Scene> creationScene();

	@FxmlScene(FxmlFile.RECOVERYKEY_RECOVER)
	Lazy<Scene> recoverScene();

	@FxmlScene(FxmlFile.RECOVERYKEY_IS_HUB_VAULT)
	Lazy<Scene> recoverIsHubVaultScene();

	default void showRecoveryKeyCreationWindow() {
		Stage stage = window();
		stage.setScene(creationScene().get());
		stage.sizeToScene();
		stage.show();
	}

	default void showRecoveryKeyRecoverWindow() {
		Stage stage = window();
		stage.setScene(recoverScene().get());
		stage.sizeToScene();
		stage.show();
	}

	default void showIsHubVaultDialogWindow() {
		Stage stage = window();
		stage.setScene(recoverIsHubVaultScene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		RecoveryKeyComponent create(@BindsInstance @RecoveryKeyWindow Vault vault,
									@BindsInstance @Named("keyRecoveryOwner") Stage owner,
									@BindsInstance @Named("recoverType") ObjectProperty<RecoverUtil.Type> recoverType);
	}

}
