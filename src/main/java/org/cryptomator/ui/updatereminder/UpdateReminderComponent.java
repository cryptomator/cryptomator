package org.cryptomator.ui.updatereminder;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.fxapp.UpdateChecker;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.time.LocalDateTime;

@UpdateReminderScoped
@Subcomponent(modules = {UpdateReminderModule.class})
public interface UpdateReminderComponent {

	@UpdateReminderWindow
	Stage window();

	@FxmlScene(FxmlFile.UPDATE_REMINDER)
	Lazy<Scene> updateReminderScene();

	Settings settings();
	UpdateChecker updateChecker();

	default void checkAndShowUpdateReminderWindow() {
		if (updateChecker().updateCheckTimeProperty().get().isBefore(LocalDateTime.now().minusDays(14)) && !settings().checkForUpdates.getValue()) {
			Stage stage = window();
			stage.setScene(updateReminderScene().get());
			stage.sizeToScene();
			stage.show();
		}
	}

	@Subcomponent.Factory
	interface Factory {
		UpdateReminderComponent create();
	}
}