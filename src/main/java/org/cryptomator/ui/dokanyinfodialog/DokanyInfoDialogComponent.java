package org.cryptomator.ui.dokanyinfodialog;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@DokanyInfoDialogScoped
@Subcomponent(modules = {DokanyInfoDialogModule.class})
public interface DokanyInfoDialogComponent {

	@DokanyInfoDialogWindow
	Stage window();

	@FxmlScene(FxmlFile.DOKANY_INFO_DIALOG)
	Lazy<Scene> dokanyInfoScene();


	default void showDokanyInfoWindow() {
		Stage stage = window();
		stage.setScene(dokanyInfoScene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		DokanyInfoDialogComponent create();
	}
}