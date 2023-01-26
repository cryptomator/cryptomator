package org.cryptomator.ui.unlock;

import org.cryptomator.common.mount.MountPointNotExistsException;
import org.cryptomator.common.mount.MountPointNotSupportedException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FormattedLabel;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

//At the current point in time only the CustomMountPointChooser may cause this window to be shown.
@UnlockScoped
public class UnlockInvalidMountPointController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final AtomicReference<Throwable> unlockException;
	private final FxApplicationWindows appWindows;
	private final ResourceBundle resourceBundle;

	public FormattedLabel dialogDescription;

	@Inject
	UnlockInvalidMountPointController(@UnlockWindow Stage window, @UnlockWindow Vault vault, @UnlockWindow AtomicReference<Throwable> unlockException, FxApplicationWindows appWindows, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.unlockException = unlockException;
		this.appWindows = appWindows;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		var e = unlockException.get();
		String translationKey = "unlock.error.customPath.description.generic";
		if (e instanceof MountPointNotSupportedException) {
			translationKey = "unlock.error.customPath.description.notSupported";
		} else if (e instanceof MountPointNotExistsException) {
			translationKey = "unlock.error.customPath.description.notExists";
		}
		dialogDescription.setFormat(resourceBundle.getString(translationKey));
		dialogDescription.setArg1(e.getMessage());
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void closeAndOpenPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.VOLUME);
		window.close();
	}

}