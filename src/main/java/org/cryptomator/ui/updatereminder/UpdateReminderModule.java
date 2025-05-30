package org.cryptomator.ui.updatereminder;

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
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class UpdateReminderModule {

	@Provides
	@UpdateReminderWindow
	@UpdateReminderScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@UpdateReminderWindow
	@UpdateReminderScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("updateReminder.title"));
		stage.setMinWidth(500);
		stage.setMinHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.UPDATE_REMINDER)
	@UpdateReminderScoped
	static Scene provideUpdateReminderScene(@UpdateReminderWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UPDATE_REMINDER);
	}


	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(UpdateReminderController.class)
	abstract FxController bindUpdateReminderController(UpdateReminderController controller);

}
