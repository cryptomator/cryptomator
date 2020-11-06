package org.cryptomator.ui.common;

import dagger.BindsInstance;
import dagger.Subcomponent;

import javax.annotation.Nullable;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

@Subcomponent(modules = {ErrorModule.class})
public interface ErrorComponent {

	Stage window();

	@FxmlScene(FxmlFile.ERROR)
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

		ErrorComponent build();

	}

}
