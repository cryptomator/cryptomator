package org.cryptomator.ui.recoverykey;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.util.Optional;

@RecoveryKeyScoped
@Subcomponent(modules = {RecoveryKeyModule.class})
public interface RecoveryKeyComponent {

	@RecoveryKeyWindow
	Stage window();

	@FxmlScene(FxmlFile.RECOVERYKEY_CREATE)
	Lazy<Scene> scene();

	default void showRecoveryKeyCreationWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
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
