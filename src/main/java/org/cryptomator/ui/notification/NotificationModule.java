package org.cryptomator.ui.notification;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageInitializer;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class NotificationModule {

	@Provides
	@NotificationWindow
	@NotificationScoped
	static Stage provideStage(StageInitializer initializer) {
		Stage stage = new Stage(StageStyle.TRANSPARENT);
		stage.setTitle("Filesystem notification"); //TODO: translate
		stage.setResizable(false);
		stage.initModality(Modality.NONE);
		stage.setAlwaysOnTop(true);
		initializer.accept(stage);
		stage.setOnShown(_ -> placeWindow(stage) );
		return stage;
	}

	static void placeWindow(Stage window) {
		if(SystemUtils.IS_OS_WINDOWS) { //place to right bottom
			var screenBounds = Screen.getPrimary().getVisualBounds();
			window.setX(screenBounds.getMaxX() - window.getWidth());
			window.setY(screenBounds.getMaxY() - window.getHeight());
		} else if(SystemUtils.IS_OS_MAC) { //place to right top
			var screenBounds = Screen.getPrimary().getVisualBounds(); //TODO: TEST
			window.setX(screenBounds.getMaxX() - window.getWidth());
			window.setY(screenBounds.getMinY() - window.getHeight());
		} else { //place to middle top
			//GNOME; KDE; etc...
			var screenBounds = Screen.getPrimary().getVisualBounds(); //TODO: TEST
			window.setX(screenBounds.getMaxX() / 2.0);
			window.setY(screenBounds.getMinY() - window.getHeight());
		}
	}

	// javafx setup

	@Provides
	@FxmlScene(FxmlFile.NOTIFICATION)
	@NotificationScoped
	static Scene provideNotificationScene(@NotificationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.NOTIFICATION);
	}

	@Provides
	@NotificationScoped
	@NotificationWindow
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Binds
	@IntoMap
	@FxControllerKey(NotificationController.class)
	abstract FxController bindNotificationController(NotificationController controller);

}
