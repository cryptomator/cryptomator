package org.cryptomator.ui.removevault;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@RemoveVaultScoped
@Subcomponent(modules = {RemoveVaultModule.class})
public interface RemoveVaultComponent {

	@RemoveVaultWindow
	Stage window();

	@FxmlScene(FxmlFile.REMOVE_VAULT)
	Lazy<Scene> scene();

	default void showRemoveVault() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@RemoveVaultWindow Vault vault);

		RemoveVaultComponent build();
	}

}
