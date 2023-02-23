package org.cryptomator.ui.forgetpassword;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@ForgetPasswordScoped
public class ForgetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ForgetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final KeychainManager keychain;
	private final BooleanProperty confirmedResult;

	@Inject
	public ForgetPasswordController(@ForgetPasswordWindow Stage window, @ForgetPasswordWindow Vault vault, KeychainManager keychain, @ForgetPasswordWindow BooleanProperty confirmedResult) {
		this.window = window;
		this.vault = vault;
		this.keychain = keychain;
		this.confirmedResult = confirmedResult;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void finish() {
		if (keychain.isSupported()) {
			try {
				keychain.deletePassphrase(vault.getId());
				LOG.debug("Forgot password for vault {}.", vault.getDisplayName());
				confirmedResult.setValue(true);
			} catch (KeychainAccessException e) {
				LOG.error("Failed to delete passphrase from system keychain.", e);
				confirmedResult.setValue(false);
			}
		} else {
			LOG.warn("Keychain not supported. Doing nothing.");
		}
		window.close();
	}
}
