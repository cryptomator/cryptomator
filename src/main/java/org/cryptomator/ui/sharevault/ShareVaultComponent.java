package org.cryptomator.ui.sharevault;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@ShareVaultScoped
@Subcomponent(modules = {ShareVaultModule.class})
public interface ShareVaultComponent {

	@ShareVaultWindow
	Stage window();

	@FxmlScene(FxmlFile.SHARE_VAULT)
	Lazy<Scene> scene();

	default void showShareVaultWindow(){
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {
		ShareVaultComponent create(@BindsInstance @ShareVaultWindow Vault vault);
	}

}
