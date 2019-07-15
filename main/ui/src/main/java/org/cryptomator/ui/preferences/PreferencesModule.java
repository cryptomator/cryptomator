package org.cryptomator.ui.preferences;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.FxApplicationScoped;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.FxControllerKey;
import org.cryptomator.ui.mainwindow.MainWindow;

@Module
public abstract class PreferencesModule {

	@Binds
	@IntoMap
	@FxControllerKey(PreferencesController.class)
	abstract FxController bindPreferencesController(PreferencesController controller);

	@Provides
	@FxApplicationScoped
	@PreferencesWindow
	static Stage providePreferencesStage(@MainWindow Stage mainWindow) {
		Stage stage = new Stage();
		stage.setMinWidth(400);
		stage.setMinHeight(300);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(mainWindow);
		return stage;
	}

}
