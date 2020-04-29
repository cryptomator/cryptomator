package org.cryptomator.ui.unlock;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Module(subcomponents = {ForgetPasswordComponent.class})
abstract class UnlockModule {

	@Provides
	@UnlockWindow
	@UnlockScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static Stage provideStage(@UnlockWindow Vault vault, @Named("windowIcons") List<Image> windowIcons) {
		Stage stage = new Stage();
		stage.setTitle(vault.getDisplayableName());
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.getIcons().addAll(windowIcons);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK)
	@UnlockScoped
	static Scene provideUnlockScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SUCCESS)
	@UnlockScoped
	static Scene provideUnlockSuccessScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock_success.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT)
	@UnlockScoped
	static Scene provideInvalidMountPointScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock_invalid_mount_point.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(UnlockController.class)
	abstract FxController bindUnlockController(UnlockController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UnlockSuccessController.class)
	abstract FxController bindUnlockSuccessController(UnlockSuccessController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UnlockInvalidMountPointController.class)
	abstract FxController bindUnlockInvalidMountPointController(UnlockInvalidMountPointController controller);

}
