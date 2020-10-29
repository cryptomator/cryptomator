package org.cryptomator.ui.recoverykey;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
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

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@RecoveryKeyWindow Vault vault);

		@BindsInstance
		Builder owner(@Named("keyRecoveryOwner") Stage owner);

		RecoveryKeyComponent build();
	}

}
