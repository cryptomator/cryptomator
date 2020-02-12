package org.cryptomator.ui.recovervault;

import dagger.Lazy;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
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

	@Inject
	public RecoverVaultController(@RecoverVaultWindow Stage window, @FxmlScene(FxmlFile.RECOVER_VAULT) Lazy<Scene> successScene) {
		this.window = window;
		this.successScene = successScene;
	}

	@FXML
	public void close() {
		window.close();
	}

}
