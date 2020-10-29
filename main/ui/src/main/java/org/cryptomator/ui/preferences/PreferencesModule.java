package org.cryptomator.ui.preferences;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module(includes = {AutoStartModule.class})
abstract class PreferencesModule {

	@Provides
	@PreferencesScoped
	static ObjectProperty<SelectedPreferencesTab> provideSelectedTabProperty() {
		return new SimpleObjectProperty<>(SelectedPreferencesTab.ANY);
	}

	@Provides
	@PreferencesWindow
	@PreferencesScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@PreferencesWindow
	@PreferencesScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("preferences.title"));
		stage.setResizable(false);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.PREFERENCES)
	@PreferencesScoped
	static Scene providePreferencesScene(@PreferencesWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/preferences.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(PreferencesController.class)
	abstract FxController bindPreferencesController(PreferencesController controller);

	@Binds
	@IntoMap
	@FxControllerKey(GeneralPreferencesController.class)
	abstract FxController bindGeneralPreferencesController(GeneralPreferencesController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UpdatesPreferencesController.class)
	abstract FxController bindUpdatesPreferencesController(UpdatesPreferencesController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VolumePreferencesController.class)
	abstract FxController bindVolumePreferencesController(VolumePreferencesController controller);

	@Binds
	@IntoMap
	@FxControllerKey(DonationKeyPreferencesController.class)
	abstract FxController bindDonationKeyPreferencesController(DonationKeyPreferencesController controller);

	@Binds
	@IntoMap
	@FxControllerKey(AboutController.class)
	abstract FxController bindAboutController(AboutController controller);

}
