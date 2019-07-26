package org.cryptomator.ui.preferences;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;

import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class PreferencesModule {

	@Provides
	@PreferencesWindow
	@PreferencesScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@PreferencesWindow
	@PreferencesScoped
	static Stage provideStage(ResourceBundle resourceBundle) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("preferences.title"));
		stage.setMinWidth(400);
		stage.setMinHeight(300);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(PreferencesController.class)
	abstract FxController bindPreferencesController(PreferencesController controller);


}
