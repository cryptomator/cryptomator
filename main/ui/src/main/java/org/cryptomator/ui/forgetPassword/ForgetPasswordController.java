package org.cryptomator.ui.forgetPassword;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

@ForgetPasswordScoped
public class ForgetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ForgetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final Optional<KeychainAccess> keychainAccess;
	private final BooleanProperty confirmedResult;

	@Inject
	public ForgetPasswordController(@ForgetPasswordWindow Stage window, @ForgetPasswordWindow Vault vault, Optional<KeychainAccess> keychainAccess, @ForgetPasswordWindow BooleanProperty confirmedResult) {
		this.window = window;
		this.vault = vault;
		this.keychainAccess = keychainAccess;
		this.confirmedResult = confirmedResult;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void finish() {
		if (keychainAccess.isPresent()) {
			try {
				keychainAccess.get().deletePassphrase(vault.getId());
				LOG.debug("Forgot password for vault {}.", vault.getDisplayableName());
				confirmedResult.setValue(true);
			} catch (KeychainAccessException e) {
				LOG.error("Failed to remove entry from system keychain.", e);
				confirmedResult.setValue(false);
			}
		}
		window.close();
	}
}
