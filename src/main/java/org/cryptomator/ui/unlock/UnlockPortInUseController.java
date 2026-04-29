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

@UnlockScoped
public class UnlockPortInUseController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;
	private final FxApplicationWindows appWindows;
	private final ObservableValue<String> port;
	private final ObservableValue<Boolean> showPreferences;
	private final ObservableValue<Boolean> showVaultOptions;

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

	public void initialize() {
		window.setTitle(String.format(resourceBundle.getString("unlock.error.title"), vault.getDisplayName()));
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

	@FXML
	public void closeAndOpenVaultOptions() {
		appWindows.showVaultOptionsWindow(vault, SelectedVaultOptionsTab.MOUNT);
		window.close();
	}

	public String getPort() {
		return port.getValue();
	}

	public ObservableValue<String> portProperty() {
		return port;
	}

	public Boolean getShowPreferences() {
		return showPreferences.getValue();
	}

	public ObservableValue<Boolean> showPreferencesProperty() {
		return showPreferences;
	}

	public Boolean getShowVaultOptions() {
		return showVaultOptions.getValue();
	}

	public ObservableValue<Boolean> showVaultOptionsProperty() {
		return showVaultOptions;
	}
}
