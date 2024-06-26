package org.cryptomator.ui.dokanysupportend;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@DokanySupportEndScoped
@Subcomponent(modules = {DokanySupportEndModule.class})
public interface DokanySupportEndComponent {

	@DokanySupportEndWindow
	Stage window();

	@FxmlScene(FxmlFile.DOKANY_SUPPORT_END)
	Lazy<Scene> dokanySupportEndScene();


	default void showDokanySupportEndWindow() {
		Stage stage = window();
		stage.setScene(dokanySupportEndScene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		DokanySupportEndComponent create();
	}
}