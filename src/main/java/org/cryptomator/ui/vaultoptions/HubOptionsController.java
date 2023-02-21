package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.stage.Stage;

public class HubOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;


	@Inject
	public HubOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window) {
		this.vault = vault;
		this.window = window;
	}
}
