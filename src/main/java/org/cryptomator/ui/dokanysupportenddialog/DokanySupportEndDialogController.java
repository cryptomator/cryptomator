package org.cryptomator.ui.dokanysupportenddialog;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;


@DokanySupportEndDialogScoped
public class DokanySupportEndDialogController implements FxController {

	private final Stage window;
	private final FxApplicationWindows applicationWindows;

	@Inject
	DokanySupportEndDialogController(@DokanySupportEndDialogWindow Stage window, FxApplicationWindows applicationWindows) {
		this.window = window;
		this.applicationWindows = applicationWindows;
	}

	@FXML
	public void close() {
		window.close();
	}

	public void openVolumePreferences() {
		applicationWindows.showPreferencesWindow(SelectedPreferencesTab.VOLUME);
	}

}