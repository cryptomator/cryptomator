package org.cryptomator.ui.controllers;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.WeakListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import javax.inject.Inject;

import org.cryptomator.ui.model.Vault;

public class MacWarningsController {

	@FXML
	private ListView<String> warningsList;

	private final Application application;
	private final ListChangeListener<? super String> macWarningsListener = this::warningsDidChange;
	private final ListChangeListener<? super String> weakMacWarningsListener = new WeakListChangeListener<>(macWarningsListener);
	private Stage stage;
	private Vault vault;

	@Inject
	public MacWarningsController(Application application) {
		this.application = application;
	}

	@FXML
	private void didClickDismissButton(ActionEvent event) {
		warningsList.getItems().removeListener(weakMacWarningsListener);
		stage.hide();
	}

	@FXML
	private void didClickMoreInformationButton(ActionEvent event) {
		application.getHostServices().showDocument("https://cryptomator.org/help.html#macWarning");
	}

	// closes this window automatically, if all warnings disappeared (e.g. due to an unmount event)
	private void warningsDidChange(Change<? extends String> change) {
		if (change.getList().isEmpty() && stage != null) {
			change.getList().removeListener(weakMacWarningsListener);
			stage.hide();
		}
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
		this.warningsList.setItems(vault.getNamesOfResourcesWithInvalidMac());
		this.warningsList.getItems().addListener(weakMacWarningsListener);
	}

}
