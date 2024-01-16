package org.cryptomator.ui.sharevault;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingStrategy;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@ShareVaultScoped
public class ShareVaultController implements FxController {
	private static final String VISIT_HUB_URL = "https://cryptomator.org/hub/";
	private static final String OPEN_HUB_URL = "https://cryptomator.org/hub/";

	private final Stage window;
	private final Lazy<Application> application;
	private final Boolean hubVault;

	@Inject
	ShareVaultController(@ShareVaultWindow Stage window, //
						 Lazy<Application> application, //
						 @ShareVaultWindow Vault vault){
		this.window = window;
		this.application = application;
		var vaultScheme = vault.getVaultConfigCache().getUnchecked().getKeyId().getScheme();
		this.hubVault = (vaultScheme.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTP) || vaultScheme.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTPS));
	}

	@FXML
	public void close(){
		window.close();
	}

	@FXML
	public void visitHub() {
		application.get().getHostServices().showDocument(VISIT_HUB_URL);
	}
	@FXML
	public void openHub() {
		application.get().getHostServices().showDocument(OPEN_HUB_URL);
	}

	public boolean isHubVault() {
		return hubVault;
	}

}
