package org.cryptomator.ui.error;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@Subcomponent(modules = {ErrorModule.class})
public interface ErrorComponent {

	Stage window();

	@FxmlScene(FxmlFile.ERROR)
	Scene scene();

	default Stage show() {
		Stage stage = window();
		stage.setScene(scene());
		stage.setMinWidth(420);
		stage.setMinHeight(300);
		stage.show();
		return stage;
	}

	@Subcomponent.Factory
	interface Factory {

		ErrorComponent create(@BindsInstance Throwable cause, @BindsInstance Stage window, @BindsInstance @Nullable Scene previousScene);
	}

}
