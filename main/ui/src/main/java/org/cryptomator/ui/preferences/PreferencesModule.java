package org.cryptomator.ui.preferences;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.cryptomator.ui.common.DefaultSceneFactory;
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
	static Stage provideStage(ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("preferences.title"));
		stage.setResizable(false);
		windowIcon.ifPresent(stage.getIcons()::add);
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

}
