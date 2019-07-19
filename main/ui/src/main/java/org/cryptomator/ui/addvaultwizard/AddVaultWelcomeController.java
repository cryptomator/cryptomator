package org.cryptomator.ui.addvaultwizard;

import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@AddVaultWizardScoped
public class AddVaultWelcomeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AddVaultWelcomeController.class);
	private final Stage window;
	private final FXMLLoaderFactory fxmlLoaders;

	@Inject
	AddVaultWelcomeController(@AddVaultWizard Stage window, @AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		this.window = window;
		this.fxmlLoaders = fxmlLoaders;
		
		LOG.info("YOYOYO");
	}

	public void createNewVault() {
		LOG.debug("AddVaultWelcomeController.createNewVault()");
		// fxmlLoaders.setScene("/fxml/addvault_new.fxml", window);
	}

	public void chooseExistingVault() {
		LOG.debug("AddVaultWelcomeController.chooseExistingVault()");
		fxmlLoaders.setScene("/fxml/addvault_existing.fxml", window);
	}
}
