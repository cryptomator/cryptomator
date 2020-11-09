package org.cryptomator.ui.wrongfilealert;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@WrongFileAlertScoped
@Subcomponent(modules = {WrongFileAlertModule.class})
public interface WrongFileAlertComponent {

	@WrongFileAlertWindow
	Stage window();

	@FxmlScene(FxmlFile.WRONGFILEALERT)
	Lazy<Scene> scene();

	default void showWrongFileAlertWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		WrongFileAlertComponent build();
	}

}
