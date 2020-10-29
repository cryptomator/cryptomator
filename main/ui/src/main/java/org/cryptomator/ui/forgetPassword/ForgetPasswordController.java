package org.cryptomator.ui.forgetPassword;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.keychain.KeychainManager;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.Optional;

@ForgetPasswordScoped
public class ForgetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ForgetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final Optional<KeychainManager> keychain;
	private final BooleanProperty confirmedResult;

	@Inject
	public ForgetPasswordController(@ForgetPasswordWindow Stage window, @ForgetPasswordWindow Vault vault, Optional<KeychainManager> keychain, @ForgetPasswordWindow BooleanProperty confirmedResult) {
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
		if (keychain.isPresent()) {
			try {
				keychain.get().deletePassphrase(vault.getId());
				LOG.debug("Forgot password for vault {}.", vault.getDisplayName());
				confirmedResult.setValue(true);
			} catch (KeychainAccessException e) {
				LOG.error("Failed to remove entry from system keychain.", e);
				confirmedResult.setValue(false);
			}
		}
		window.close();
	}
}
