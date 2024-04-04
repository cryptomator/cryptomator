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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
		try {
			var dateTime = LocalDateTime.parse(settings().lastUpdateCheck.get(), DateTimeFormatter.ISO_DATE_TIME);
			if (dateTime.isBefore(LocalDateTime.now().minusDays(14)) && !settings().checkForUpdates.getValue()) {
				Stage stage = window();
				stage.setScene(updateReminderScene().get());
				stage.sizeToScene();
				stage.show();
			}
		} catch (DateTimeParseException e) {
			LOG.error("The date/time format is invalid:" + settings().lastUpdateCheck.get(), e);
		}
	}

	@Subcomponent.Factory
	interface Factory {

		UpdateReminderComponent create();
	}
}