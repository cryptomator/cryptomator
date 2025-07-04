package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
	private final RecoveryKeyComponent.Factory recoveryKeyWindow;
	private final ResourceBundle resourceBundle;

	@FXML
	private CheckBox restoreInsteadCheckBox;
	@FXML
	private Button forwardButton;

	@Inject
	public ChooseMasterkeyFileController(@KeyLoading Stage window, //
										 @KeyLoading Vault vault, //
										 CompletableFuture<Path> result, //
										 RecoveryKeyComponent.Factory recoveryKeyWindow, //
										 ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.result = result;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.resourceBundle = resourceBundle;
		this.window.setOnHiding(this::windowClosed);
	}

	@FXML
	private void initialize() {
		restoreInsteadCheckBox.selectedProperty().addListener((_, _, newVal) -> {
			if (newVal) {
				forwardButton.setText(resourceBundle.getString("addvaultwizard.existing.restore"));
				forwardButton.setOnAction(_ -> restoreMasterkey());
			} else {
				forwardButton.setText(resourceBundle.getString("generic.button.choose"));
				forwardButton.setOnAction(_ -> proceed());
			}
		});
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}

	@FXML
	void restoreMasterkey() {
		window.close();
		recoveryKeyWindow.create(vault, window, new SimpleObjectProperty<>(RecoveryActionType.RESTORE_MASTERKEY)).showOnboardingDialogWindow();
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

	public String getDisplayName() {
		return vault.getDisplayName();
	}

}
