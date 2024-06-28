package org.cryptomator.ui.preferences;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
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
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@PreferencesWindow
	@PreferencesScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("preferences.title"));
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.PREFERENCES)
	@PreferencesScoped
	static Scene providePreferencesScene(@PreferencesWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.PREFERENCES);
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
	@FxControllerKey(InterfacePreferencesController.class)
	abstract FxController bindInterfacePreferencesController(InterfacePreferencesController controller);

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
	@FxControllerKey(SupporterCertificateController.class)
	abstract FxController bindSupporterCertificatePreferencesController(SupporterCertificateController controller);

	@Binds
	@IntoMap
	@FxControllerKey(AboutController.class)
	abstract FxController bindAboutController(AboutController controller);

	@Binds
	@IntoMap
	@FxControllerKey(NetworkPreferencesController.class)
	abstract FxController bindNetworkPreferencesController(NetworkPreferencesController controller);

}
