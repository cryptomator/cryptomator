package org.cryptomator.ui.sharevault;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingStrategy;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;

@ShareVaultScoped
public class ShareVaultController implements FxController {

	private static final String SCHEME_PREFIX = "hub+";
	private static final String VISIT_HUB_URL = "https://cryptomator.org/hub/";
	private static final String BEST_PRACTICES_URL = "https://docs.cryptomator.org/en/latest/security/best-practices/#sharing-of-vaults";

	private final Stage window;
	private final Lazy<Application> application;
	private final Vault vault;
	private final Boolean hubVault;

	@Inject
	ShareVaultController(@ShareVaultWindow Stage window, //
						 Lazy<Application> application, //
						 @ShareVaultWindow Vault vault) {
		this.window = window;
		this.application = application;
		this.vault = vault;
		var vaultKeyLoader = vault.getVaultSettings().lastKnownKeyLoader.get();
		this.hubVault = (vaultKeyLoader.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTP) || vaultKeyLoader.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTPS));
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void visitHub() {
		application.get().getHostServices().showDocument(VISIT_HUB_URL);
	}

	@FXML
	public void openHub() {
		application.get().getHostServices().showDocument(getHubUri(vault).toString());
	}

	@FXML
	public void visitBestPractices() {
		application.get().getHostServices().showDocument(BEST_PRACTICES_URL);
	}

	private static URI getHubUri(Vault vault) {
		try {
			var keyID = new URI(vault.getVaultConfigCache().get().getKeyId().toString());
			assert keyID.getScheme().startsWith(SCHEME_PREFIX);
			return new URI(keyID.getScheme().substring(SCHEME_PREFIX.length()) + "://" + keyID.getHost() + "/app/vaults");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from params known to be valid", e);
		}
	}

	public boolean isHubVault() {
		return hubVault;
	}

}
