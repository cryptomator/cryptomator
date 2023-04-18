package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.convertvault.ConvertVaultComponent;

import javax.inject.Inject;
import javafx.stage.Stage;

public class HubOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	private final ConvertVaultComponent.Factory convertVaultFactory;


	@Inject
	public HubOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ConvertVaultComponent.Factory convertVaultFactory) {
		this.vault = vault;
		this.window = window;
		this.convertVaultFactory = convertVaultFactory;
	}

	public void startConversion() {
		convertVaultFactory.create(vault,window).showHubToPasswordWindow();
	}
}
