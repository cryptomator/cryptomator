package org.cryptomator.ui.common;

import dagger.BindsInstance;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.annotation.Nullable;

@Subcomponent(modules = {ErrorModule.class})
public interface ErrorComponent {

	Stage window();

	@FxmlScene(FxmlFile.ERROR)
	Scene scene();

	default void showErrorScene() {
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
