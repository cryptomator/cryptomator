package org.cryptomator.ui.updatereminder;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@UpdateReminderScoped
@Subcomponent(modules = {UpdateReminderModule.class})
public interface UpdateReminderComponent {

	@UpdateReminderWindow
	Stage window();

	@FxmlScene(FxmlFile.UPDATE_REMINDER)
	Lazy<Scene> updateReminderScene();

	default void showUpdateReminderWindow() {
		Stage stage = window();
		stage.setScene(updateReminderScene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		UpdateReminderComponent create();
	}
}