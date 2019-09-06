package org.cryptomator.ui.migration;

import dagger.Lazy;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;

@MigrationScoped
public class MigrationStartController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final Lazy<Scene> successScene;

	@Inject
	public MigrationStartController(@MigrationWindow Stage window, @MigrationWindow Vault vault, @FxmlScene(FxmlFile.MIGRATION_START) Lazy<Scene> successScene) {
		this.window = window;
		this.vault = vault;
		this.successScene = successScene;
	}

	public void initialize() {
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void proceed() {

	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}
