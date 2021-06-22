package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.health.HealthCheckComponent;

import javax.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@VaultOptionsScoped
public class HealthVaultOptionsController implements FxController {

	@Inject
	public HealthVaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, HealthCheckComponent.Builder healthCheckWindow) {
		this.window = window;
		this.vault = vault;
		this.healthCheckWindow = healthCheckWindow;
	}

	@FXML
	public void startHealthCheck(ActionEvent event) {
		healthCheckWindow.vault(vault).build().showHealthCheckWindow();
	}

	private final Stage window;
	private final Vault vault;
	private final HealthCheckComponent.Builder healthCheckWindow;
}
