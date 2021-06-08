package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.scene.Scene;
import javafx.stage.Stage;

@AddVaultWizardScoped
public class AddVaultWelcomeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AddVaultWelcomeController.class);
	private final Stage window;
	private final Lazy<Scene> chooseExistingVaultScene;
	private final Lazy<Scene> createNewVaultScene;

	@Inject
	AddVaultWelcomeController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_EXISTING) Lazy<Scene> chooseExistingVaultScene, @FxmlScene(FxmlFile.ADDVAULT_NEW_NAME) Lazy<Scene> createNewVaultScene) {
		this.window = window;
		this.chooseExistingVaultScene = chooseExistingVaultScene;
		this.createNewVaultScene = createNewVaultScene;
	}

	public void createNewVault() {
		LOG.debug("AddVaultWelcomeController.createNewVault()");
		window.setScene(createNewVaultScene.get());
	}

	public void chooseExistingVault() {
		LOG.debug("AddVaultWelcomeController.chooseExistingVault()");
		window.setScene(chooseExistingVaultScene.get());
	}
}
