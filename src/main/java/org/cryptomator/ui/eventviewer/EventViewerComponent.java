package org.cryptomator.ui.eventviewer;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@EventViewerScoped
@Subcomponent(modules = {EventViewerModule.class})
public interface EventViewerComponent {

	@EventViewerWindow
	Stage window();

	@FxmlScene(FxmlFile.EVENT_VIEWER)
	Lazy<Scene> scene();

	default void showEventViewerWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		EventViewerComponent create(@BindsInstance @Named("owner") Stage owner);
	}
}
