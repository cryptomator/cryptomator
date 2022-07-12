package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import java.net.URI;

@MainWindowScoped
public class WelcomeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);
	private static final String GETTING_STARTED_PATH = "desktop/getting-started";

	private final Application application;
	private final BooleanBinding noVaultPresent;
	private final URI gettingStartedUri;

	@Inject
	public WelcomeController(Application application, ObservableList<Vault> vaults, @Named("docUri") URI docUri) {
		this.application = application;
		this.noVaultPresent = Bindings.isEmpty(vaults);
		this.gettingStartedUri = docUri.resolve(GETTING_STARTED_PATH);
	}

	@FXML
	public void visitGettingStartedGuide() {
		LOG.trace("Opening {}", gettingStartedUri);
		application.getHostServices().showDocument(gettingStartedUri.toString());
	}

	/* Getter/Setter */

	public BooleanBinding noVaultPresentProperty() {
		return noVaultPresent;
	}

	public boolean isNoVaultPresent() {
		return noVaultPresent.get();
	}

}
