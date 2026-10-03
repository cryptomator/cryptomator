package org.cryptomator.ui.unlock;

import org.cryptomator.common.mount.PortAlreadyInUseException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.ResourceBundle;

/**
 * Controller for the unlock error shown when Cryptomator's local loopback server can not bind its configured TCP port.
 */
@UnlockScoped
public class UnlockPortInUseController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;
	private final FxApplicationWindows appWindows;
	private final ObservableValue<String> port;
	private final ObservableValue<Boolean> showPreferences;
	private final ObservableValue<Boolean> showVaultOptions;

	/**
	 * Creates a controller that exposes the occupied port and the matching settings action for the FXML view.
	 *
	 * @param window The unlock window displaying the error.
	 * @param vault The vault whose unlock failed.
	 * @param resourceBundle Localized UI strings.
	 * @param appWindows Window service used to open settings or vault options.
	 * @param portAlreadyInUseException The typed mount failure backing this error view.
	 */
	@Inject
	UnlockPortInUseController(@UnlockWindow Stage window, //
							  @UnlockWindow Vault vault, //
							  ResourceBundle resourceBundle, //
							  FxApplicationWindows appWindows, //
							  @UnlockWindow ObjectProperty<PortAlreadyInUseException> portAlreadyInUseException) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
		this.appWindows = appWindows;
		this.port = portAlreadyInUseException.map(e -> Integer.toString(e.getPort()));
		this.showPreferences = portAlreadyInUseException.map(PortAlreadyInUseException::isDefaultPort);
		this.showVaultOptions = portAlreadyInUseException.map(e -> !e.isDefaultPort());
	}

	/**
	 * Sets the unlock window title after FXML loading has completed.
	 */
	public void initialize() {
		window.setTitle(String.format(resourceBundle.getString("unlock.error.title"), vault.getDisplayName()));
	}

	/**
	 * Closes the unlock error window.
	 */
	@FXML
	public void close() {
		window.close();
	}

	/**
	 * Closes the error window and opens the global volume preferences.
	 */
	@FXML
	public void closeAndOpenPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.VOLUME);
		window.close();
	}

	/**
	 * Closes the error window and opens the vault-specific mount options.
	 */
	@FXML
	public void closeAndOpenVaultOptions() {
		appWindows.showVaultOptionsWindow(vault, SelectedVaultOptionsTab.MOUNT);
		window.close();
	}

	/**
	 * @return The occupied TCP port as display text.
	 */
	public String getPort() {
		return port.getValue();
	}

	/**
	 * @return Observable occupied TCP port display text.
	 */
	public ObservableValue<String> portProperty() {
		return port;
	}

	/**
	 * @return {@code true} if the global volume preferences button should be shown.
	 */
	public Boolean getShowPreferences() {
		return showPreferences.getValue();
	}

	/**
	 * @return Observable visibility state for the global volume preferences button.
	 */
	public ObservableValue<Boolean> showPreferencesProperty() {
		return showPreferences;
	}

	/**
	 * @return {@code true} if the vault mount options button should be shown.
	 */
	public Boolean getShowVaultOptions() {
		return showVaultOptions.getValue();
	}

	/**
	 * @return Observable visibility state for the vault mount options button.
	 */
	public ObservableValue<Boolean> showVaultOptionsProperty() {
		return showVaultOptions;
	}
}
