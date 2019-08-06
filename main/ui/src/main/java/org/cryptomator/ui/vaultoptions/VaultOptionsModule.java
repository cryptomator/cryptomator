package org.cryptomator.ui.vaultoptions;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class VaultOptionsModule {

	@Provides
	@VaultOptionsWindow
	@VaultOptionsScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@VaultOptionsWindow
	@VaultOptionsScoped
	static Stage provideStage(@MainWindow Stage owner, @VaultOptionsWindow Vault vault, ResourceBundle resourceBundle) {
		Stage stage = new Stage();
		stage.setTitle(vault.getDisplayableName());
		// stage.setTitle(resourceBundle.getString("vaultOptions.title"));
		stage.setMinWidth(400);
		stage.setMinHeight(300);
		stage.initStyle(StageStyle.DECORATED);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.VAULT_OPTIONS)
	@VaultOptionsScoped
	static Scene provideVaultOptionsScene(@VaultOptionsWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/vault_options.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(VaultOptionsController.class)
	abstract FxController bindVaultOptionsController(VaultOptionsController controller);

	@Binds
	@IntoMap
	@FxControllerKey(GeneralVaultOptionsController.class)
	abstract FxController bindGeneralVaultOptionsController(GeneralVaultOptionsController controller);

	@Binds
	@IntoMap
	@FxControllerKey(MountOptionsController.class)
	abstract FxController bindMountOptionsController(MountOptionsController controller);

}
