package org.cryptomator.ui.addvaultwizard;

import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private final FXMLLoaderFactory fxmlLoaders;
	private final Stage window;

	@Inject
	ChooseExistingVaultController(@AddVaultWizard Stage window, @AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		this.window = window;
		this.fxmlLoaders = fxmlLoaders;
	}

	public void chooseFile(ActionEvent actionEvent) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Masterkey File");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		final File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			window.setWidth(100);
		}
	}

	public void goBack(ActionEvent actionEvent) throws IOException {
		fxmlLoaders.setScene("/fxml/addvault_welcome.fxml", window);

	}

	public void confirm(ActionEvent actionEvent) {
		window.close();
	}
}
