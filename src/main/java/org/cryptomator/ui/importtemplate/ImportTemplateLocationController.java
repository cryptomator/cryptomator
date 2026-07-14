package org.cryptomator.ui.importtemplate;

import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.nio.file.Path;

@ImportTemplateScoped
public class ImportTemplateLocationController implements FxController {

	private final Stage window;
	private final String vaultName;
	private final byte[] template;
	private final ObjectProperty<Path> vaultPath;

	@Inject
	ImportTemplateLocationController(@ImportTemplateWindow Stage window, //
									 @Named("vaultName") String vaultName, //
									 @Named("vaultTemplate") byte[] template, //
									 ObjectProperty<Path> vaultPath) {
		this.window = window;
		this.vaultName = vaultName;
		this.template = template;
		this.vaultPath = vaultPath;
	}

	@FXML
	public void initialize() {
		// TODO (import-template step 4b): location presets, path validation, finish() with spinner + Tasks.runOnce
	}

	public String getVaultName() {
		return vaultName;
	}

}
