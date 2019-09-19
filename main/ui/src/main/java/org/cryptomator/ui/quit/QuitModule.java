package org.cryptomator.ui.quit;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
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
	static Stage provideStage(@Named("windowIcon") Optional<Image> windowIcon) {
		Stage stage = new Stage();
		stage.setMinWidth(300);
		stage.setMinHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.QUIT)
	@QuitScoped
	static Scene provideUnlockScene(@QuitWindow FXMLLoaderFactory fxmlLoaders, @QuitWindow Stage window) {
		Scene scene = fxmlLoaders.createScene("/fxml/quit.fxml");

		KeyCombination cmdW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
		scene.getAccelerators().put(cmdW, window::close);

		return scene;
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(QuitController.class)
	abstract FxController bindQuitController(QuitController controller);

}
