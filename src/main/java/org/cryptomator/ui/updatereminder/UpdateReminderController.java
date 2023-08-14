package org.cryptomator.ui.updatereminder;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@UpdateReminderScoped
public class UpdateReminderController implements FxController {

	private final Stage window;
	private final Settings settings;
	private final UpdateChecker updateChecker;


	@Inject
	UpdateReminderController(@UpdateReminderWindow Stage window, Settings settings, UpdateChecker updateChecker) {
		this.window = window;
		this.settings = settings;
		this.updateChecker = updateChecker;
	}

	@FXML
	public void cancel() {
		settings.lastUpdateCheck.set(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		window.close();
	}

	@FXML
	public void once() {
		settings.lastUpdateCheck.set(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		updateChecker.checkForUpdatesNow();
		window.close();
	}

	@FXML
	public void automatically() {
		settings.lastUpdateCheck.set(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		updateChecker.checkForUpdatesNow();
		settings.checkForUpdates.set(true);
		window.close();
	}

}