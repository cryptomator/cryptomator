package org.cryptomator.ui.quit;

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
import org.cryptomator.ui.unlock.UnlockController;

import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class QuitModule {

	@Provides
	@QuitWindow
	@QuitScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@QuitWindow
	@QuitScoped
	static Stage provideStage() {
		Stage stage = new Stage();
		stage.setMinWidth(300);
		stage.setMinHeight(200);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.QUIT)
	@QuitScoped
	static Scene provideUnlockScene(@QuitWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/quit.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(QuitController.class)
	abstract FxController bindQuitController(QuitController controller);

}
