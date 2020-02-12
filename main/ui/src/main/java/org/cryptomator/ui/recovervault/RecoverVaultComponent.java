package org.cryptomator.ui.recovervault;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;

@RecoverVaultScoped
@Subcomponent(modules = {RecoverVaultModule.class})
public interface RecoverVaultComponent {

	@RecoverVaultWindow
	Stage window();

	@FxmlScene(FxmlFile.RECOVER_VAULT)
	Lazy<Scene> scene();

	default void showRecoverVaultWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@RecoverVaultWindow Vault vault);

		@BindsInstance
		Builder owner(@Named("recoverVaultOwner") Stage owner);

		RecoverVaultComponent build();
	}
}
