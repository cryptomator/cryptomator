package org.cryptomator.ui.unlock;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.unlock.UnlockModule.MasterkeyFileProvision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

@UnlockScoped
public class UnlockSelectMasterkeyFileController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockSelectMasterkeyFileController.class);

	private final Stage window;
	private final AtomicReference<Path> masterkeyPath;
	private final UserInteractionLock<MasterkeyFileProvision> masterkeyFileProvisionLock;
	private final ResourceBundle resourceBundle;

	@Inject
	public UnlockSelectMasterkeyFileController(@UnlockWindow Stage window, @Named("userProvidedMasterkeyPath") AtomicReference<Path> masterkeyPath, UserInteractionLock<MasterkeyFileProvision> masterkeyFileProvisionLock, ResourceBundle resourceBundle) {
		this.window = window;
		this.masterkeyPath = masterkeyPath;
		this.masterkeyFileProvisionLock = masterkeyFileProvisionLock;
		this.resourceBundle = resourceBundle;
		this.window.setOnHiding(this::windowClosed);
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (masterkeyFileProvisionLock.awaitingInteraction().get()) {
			LOG.debug("Unlock canceled by user.");
			masterkeyFileProvisionLock.interacted(MasterkeyFileProvision.CANCELED);
		}
	}

	@FXML
	public void proceed() {
		LOG.trace("proceed()");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("unlock.chooseMasterkey.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		File masterkeyFile = fileChooser.showOpenDialog(window);
		if (masterkeyFile != null) {
			LOG.debug("Chose masterkey file: {}", masterkeyFile);
			masterkeyPath.set(masterkeyFile.toPath());
			masterkeyFileProvisionLock.interacted(MasterkeyFileProvision.MASTERKEYFILE_PROVIDED);
		}
	}

}
