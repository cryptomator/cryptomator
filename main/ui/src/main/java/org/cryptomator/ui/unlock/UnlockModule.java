package org.cryptomator.ui.unlock;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class UnlockModule {

	@Provides
	@UnlockWindow
	@UnlockScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static Stage provideStage() {
		Stage stage = new Stage();
		stage.setMinWidth(300);
		stage.setMinHeight(200);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK)
	@UnlockScoped
	static Scene provideUnlockScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock2.fxml"); // TODO rename fxml file
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SUCCESS)
	@UnlockScoped
	static Scene provideUnlockSuccessScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock_success.fxml");
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


}
