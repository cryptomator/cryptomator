package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javafx.stage.Stage;

public class NoKeychainController implements FxController {

	private final Stage window;
	private final FxApplicationWindows appWindows;

	@Inject
	public NoKeychainController(@KeyLoading Stage window, FxApplicationWindows appWindows) {
		this.window = window;
		this.appWindows = appWindows;
	}


	public void cancel() {
		window.close();
	}

	public void openPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.GENERAL);
		window.close();
	}
}
