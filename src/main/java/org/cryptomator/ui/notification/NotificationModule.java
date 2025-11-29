package org.cryptomator.ui.notification;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageInitializer;
import org.cryptomator.ui.quit.QuitForcedController;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class NotificationModule {

	@Provides
	@NotificationWindow
	@NotificationScoped
	static Stage provideStage(StageInitializer initializer, @FxmlScene(FxmlFile.NOTIFICATION) Scene notificationScene) {
		Stage stage = new Stage(StageStyle.UTILITY);
		stage.setTitle("Filesystem notification"); //TODO: translate
		stage.setResizable(false);
		stage.initModality(Modality.NONE);
		initializer.accept(stage);
		stage.setScene(notificationScene);
		stage.sizeToScene();
		return stage;
	}


	// javafx setup

	@Provides
	@FxmlScene(FxmlFile.NOTIFICATION)
	@NotificationScoped
	static Scene provideNotificationScene(FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.NOTIFICATION);
	}

	@Provides
	@NotificationScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Binds
	@IntoMap
	@FxControllerKey(NotificationController.class)
	abstract FxController bindNotificationController(NotificationController controller);

}
