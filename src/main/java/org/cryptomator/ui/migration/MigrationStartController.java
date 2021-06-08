package org.cryptomator.ui.migration;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

@MigrationScoped
public class MigrationStartController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final Lazy<Scene> runMigrationScene;

	@Inject
	public MigrationStartController(@MigrationWindow Stage window, @MigrationWindow Vault vault, @FxmlScene(FxmlFile.MIGRATION_RUN) Lazy<Scene> runMigrationScene) {
		this.window = window;
		this.vault = vault;
		this.runMigrationScene = runMigrationScene;
	}

	public void initialize() {
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void proceed() {
		window.setScene(runMigrationScene.get());
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}
