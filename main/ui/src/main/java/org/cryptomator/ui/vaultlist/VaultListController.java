package org.cryptomator.ui.vaultlist;

import javafx.fxml.FXML;
import org.cryptomator.ui.FxApplicationScoped;
import org.cryptomator.ui.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@FxApplicationScoped
public class VaultListController implements FxController {
	
	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	@Inject
	public VaultListController() {
	}

	@FXML
	public void initialize() {
		LOG.debug("init VaultListController");
	}

}
