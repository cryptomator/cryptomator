package org.cryptomator.ui.vaultstatistics;

import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private final Stage window;
	private final Vault vault;

	@Inject
	public VaultStatisticsController(@VaultStatisticsWindow Stage window, @VaultStatisticsWindow Vault vault) {
		this.window = window;
		this.vault = vault;
	}
}
