package org.cryptomator.ui.error;

import dagger.BindsInstance;

import javax.annotation.Nullable;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public interface ErrorComponentBase {

	@ErrorReport
	Stage window();

	@Named("errorScene")
	Scene errorScene();

	@ErrorReport
	Throwable cause();

	@ErrorReport
	@Nullable
	Scene previousScene();

	default void showErrorScene() {
		if (Platform.isFxApplicationThread()) {
			show();
		} else {
			Platform.runLater(this::show);
		}
	}

	private void show() {
		Stage stage = window();
		stage.setScene(errorScene());
		stage.show();
	}

	interface BuilderBase<B, C> {

		@BindsInstance
		B window(@ErrorReport Stage window);

		@BindsInstance
		B cause(@ErrorReport Throwable cause);

		@BindsInstance
		B returnToScene(@ErrorReport @Nullable Scene returnScene);

		C build();
	}
}