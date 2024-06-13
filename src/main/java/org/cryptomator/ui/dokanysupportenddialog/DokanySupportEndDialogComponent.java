package org.cryptomator.ui.dokanysupportenddialog;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@DokanySupportEndDialogScoped
@Subcomponent(modules = {DokanySupportEndDialogModule.class})
public interface DokanySupportEndDialogComponent {

	@DokanySupportEndDialogWindow
	Stage window();

	@FxmlScene(FxmlFile.DOKANY_SUPPORT_END_DIALOG)
	Lazy<Scene> dokanySupportEndScene();


	default void showDokanySupportEndWindow() {
		Stage stage = window();
		stage.setScene(dokanySupportEndScene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		DokanySupportEndDialogComponent create();
	}
}