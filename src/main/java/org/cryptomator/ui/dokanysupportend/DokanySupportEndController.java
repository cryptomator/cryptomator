package org.cryptomator.ui.dokanysupportend;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;


@DokanySupportEndScoped
public class DokanySupportEndController implements FxController {

	private final Stage window;
	private final FxApplicationWindows applicationWindows;

	@Inject
	DokanySupportEndController(@DokanySupportEndWindow Stage window, FxApplicationWindows applicationWindows) {
		this.window = window;
		this.applicationWindows = applicationWindows;
	}

	@FXML
	public void close() {
		window.close();
	}

	public void openVolumePreferences() {
		applicationWindows.showPreferencesWindow(SelectedPreferencesTab.VOLUME);
		window.close();
	}

}