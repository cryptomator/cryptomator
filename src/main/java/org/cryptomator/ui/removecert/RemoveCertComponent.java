package org.cryptomator.ui.removecert;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@RemoveCertScoped
@Subcomponent(modules = {RemoveCertModule.class})
public interface RemoveCertComponent {

	@RemoveCertWindow
	Stage window();

	@FxmlScene(FxmlFile.REMOVE_CERT)
	Lazy<Scene> scene();

	default void showRemoveCert() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {
		RemoveCertComponent build();
	}

}
