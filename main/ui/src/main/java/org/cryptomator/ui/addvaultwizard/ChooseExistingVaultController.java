package org.cryptomator.ui.addvaultwizard;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import java.io.File;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private final FXMLLoaderFactory fxmlLoaders;
	private final Stage window;

	public TextField textField;

	@Inject
	ChooseExistingVaultController(@AddVaultWizard Stage window, @AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		this.window = window;
		this.fxmlLoaders = fxmlLoaders;
	}

	@FXML
	public void chooseFile() {
		FileChooser fileChooser = new FileChooser();
		//TODO: Title is part of the localization. => inject resource bundle and get correct title
		fileChooser.setTitle("Open Masterkey File");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		final File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			textField.setText(file.getAbsolutePath());
		}
	}

	@FXML
	public void goBack() {
		fxmlLoaders.setScene("/fxml/addvault_welcome.fxml", window);

	}

	@FXML
	public void confirm() {
		window.close();
	}
}
