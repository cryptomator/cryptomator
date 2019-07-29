package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@AddVaultWizardScoped
public class AddVaultWelcomeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AddVaultWelcomeController.class);
	private final Stage window;
	private final Lazy<Scene> chooseExistingVaultScene;

	@Inject
	AddVaultWelcomeController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_EXISTING) Lazy<Scene> chooseExistingVaultScene) {
		this.window = window;
		this.chooseExistingVaultScene = chooseExistingVaultScene;
	}

	public void createNewVault() {
		LOG.debug("AddVaultWelcomeController.createNewVault()");
		// fxmlLoaders.setScene("/fxml/addvault_new.fxml", window);
	}

	public void chooseExistingVault() {
		LOG.debug("AddVaultWelcomeController.chooseExistingVault()");
		window.setScene(chooseExistingVaultScene.get());
	}
}
