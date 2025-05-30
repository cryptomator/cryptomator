package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

@MainWindowScoped
public class WelcomeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);
	private static final String GETTING_STARTED_URI = "https://docs.cryptomator.org/desktop/getting-started/";

	private final Application application;
	private final BooleanBinding noVaultPresent;

	@Inject
	public WelcomeController(Application application, ObservableList<Vault> vaults) {
		this.application = application;
		this.noVaultPresent = Bindings.isEmpty(vaults);
	}

	@FXML
	public void visitGettingStartedGuide() {
		LOG.trace("Opening {}", GETTING_STARTED_URI);
		application.getHostServices().showDocument(GETTING_STARTED_URI);
	}

	/* Getter/Setter */

	public BooleanBinding noVaultPresentProperty() {
		return noVaultPresent;
	}

	public boolean isNoVaultPresent() {
		return noVaultPresent.get();
	}

}
