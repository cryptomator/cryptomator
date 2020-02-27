package org.cryptomator.ui.migration;

import dagger.Lazy;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;

@MigrationScoped
public class MigrationGenericErrorController implements FxController {

	private final Stage window;
	private final Lazy<Scene> startScene;

	@Inject
	MigrationGenericErrorController(@MigrationWindow Stage window, @FxmlScene(FxmlFile.MIGRATION_START) Lazy<Scene> startScene) {
		this.window = window;
		this.startScene = startScene;
	}

	@FXML
	public void back() {
		window.setScene(startScene.get());
	}
}
