package org.cryptomator.ui.recovervault;

import dagger.Lazy;
import javafx.beans.Observable;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@RecoverVaultScoped
public class RecoverVaultController implements FxController {


	private static final Logger LOG = LoggerFactory.getLogger(RecoverVaultController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final Vault vault;
	private StringProperty recoveryKey;

	public TextArea textarea;

	@Inject
	public RecoverVaultController(@RecoverVaultWindow Stage window, @FxmlScene(FxmlFile.RECOVER_VAULT) Lazy<Scene> successScene, @RecoverVaultWindow Vault vault, @RecoverVaultWindow StringProperty recoveryKey) {
		this.window = window;
		this.successScene = successScene;
		this.vault = vault;
		this.recoveryKey = recoveryKey;
	}

	@FXML
	public void initialize() {
		textarea.getParagraphs().addListener(this::updateRecoveryKeyProperty);
	}


	private void updateRecoveryKeyProperty(@SuppressWarnings("unused") Observable observable) {
		recoveryKey.set(textarea.getText());
	}


	@FXML
	public void close() {
		window.close();
	}

	public void recoverData(ActionEvent actionEvent) {
		//TODO: CryptoAPI call, show progress bar
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}
