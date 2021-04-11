package org.cryptomator.ui.error;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.annotation.Nullable;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

@Subcomponent(modules = {ErrorModule.class})
public interface GenericErrorComponent {

	Stage window();

	@FxmlScene(FxmlFile.GENERIC_ERROR)
	Scene scene();

	default void showErrorScene() {
		if (Platform.isFxApplicationThread()) {
			show();
		} else {
			Platform.runLater(this::show);
		}
	}

	private void show() {
		Stage stage = window();
		stage.setScene(scene());
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder cause(Throwable cause);

		@BindsInstance
		Builder window(Stage window);

		@BindsInstance
		Builder returnToScene(@Nullable Scene previousScene);

		GenericErrorComponent build();

	}

}
