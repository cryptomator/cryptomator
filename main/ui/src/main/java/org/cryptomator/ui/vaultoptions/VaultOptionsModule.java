package org.cryptomator.ui.vaultoptions;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Module(subcomponents = {ChangePasswordComponent.class, RecoveryKeyComponent.class})
abstract class VaultOptionsModule {

	@Provides
	@VaultOptionsWindow
	@VaultOptionsScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@VaultOptionsWindow
	@VaultOptionsScoped
	static Stage provideStage(StageFactory factory, @MainWindow Stage owner, @VaultOptionsWindow Vault vault) {
		Stage stage = factory.create();
		stage.setTitle(vault.getDisplayableName());
		stage.setResizable(true);
		stage.setMinWidth(400);
		stage.setMinHeight(300);
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

	@Binds
	@IntoMap
	@FxControllerKey(MasterkeyOptionsController.class)
	abstract FxController bindMasterkeyOptionsController(MasterkeyOptionsController controller);

}
