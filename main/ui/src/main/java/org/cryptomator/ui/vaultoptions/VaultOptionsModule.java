package org.cryptomator.ui.vaultoptions;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module(subcomponents = {ChangePasswordComponent.class, RecoveryKeyComponent.class})
abstract class VaultOptionsModule {

	@Provides
	@VaultOptionsWindow
	@VaultOptionsScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@VaultOptionsWindow
	@VaultOptionsScoped
	static Stage provideStage(StageFactory factory, @MainWindow Stage owner, @VaultOptionsWindow Vault vault) {
		Stage stage = factory.create();
		stage.setTitle(vault.getDisplayName());
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
	static Scene provideVaultOptionsScene(@VaultOptionsWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.VAULT_OPTIONS);
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
