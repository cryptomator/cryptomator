package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_GLOB;

@ChooseMasterkeyFileScoped
public class ChooseMasterkeyFileController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChooseMasterkeyFileController.class);

	private final Stage window;
	private final Vault vault;
	private final CompletableFuture<Path> result;
	private final ResourceBundle resourceBundle;

	@Inject
	public ChooseMasterkeyFileController(@KeyLoading Stage window, @KeyLoading Vault vault, CompletableFuture<Path> result, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.result = result;
		this.resourceBundle = resourceBundle;
		this.window.setOnHiding(this::windowClosed);
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}

	@FXML
	public void proceed() {
		LOG.trace("proceed()");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("unlock.chooseMasterkey.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(resourceBundle.getString("unlock.chooseMasterkey.filePickerMimeDesc"), CRYPTOMATOR_FILENAME_GLOB));
		File masterkeyFile = fileChooser.showOpenDialog(window);
		if (masterkeyFile != null) {
			LOG.debug("Chose masterkey file: {}", masterkeyFile);
			result.complete(masterkeyFile.toPath());
		}
	}

	//--- Setter & Getter ---

	public String getDisplayName(){
		return vault.getDisplayName();
	}

}
