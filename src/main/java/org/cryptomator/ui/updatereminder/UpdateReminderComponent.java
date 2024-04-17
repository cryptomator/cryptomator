package org.cryptomator.ui.updatereminder;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.time.LocalDate;

@UpdateReminderScoped
@Subcomponent(modules = {UpdateReminderModule.class})
public interface UpdateReminderComponent {

	Logger LOG = LoggerFactory.getLogger(UpdateReminderComponent.class);

	@UpdateReminderWindow
	Stage window();

	@FxmlScene(FxmlFile.UPDATE_REMINDER)
	Lazy<Scene> updateReminderScene();

	Settings settings();

	default void checkAndShowUpdateReminderWindow() {
		if (LocalDate.parse(settings().lastUpdateReminder.get()).isBefore(LocalDate.now().minusDays(14)) && !settings().checkForUpdates.getValue()) {
			settings().lastUpdateReminder.set(LocalDate.now().toString());
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