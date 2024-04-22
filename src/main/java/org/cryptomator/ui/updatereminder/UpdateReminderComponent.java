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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

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
		if (settings().lastUpdateReminder.get().before(Date.from(Instant.now().minus(Duration.ofDays(14)))) && !settings().checkForUpdates.getValue()) {
			settings().lastUpdateReminder.set(new Date());
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