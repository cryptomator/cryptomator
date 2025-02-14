package org.cryptomator.ui.eventview;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@EventViewScoped
@Subcomponent(modules = {EventViewModule.class})
public interface EventViewComponent {

	@EventViewWindow
	Stage window();

	@FxmlScene(FxmlFile.EVENT_VIEW)
	Lazy<Scene> scene();

	default void showEventViewerWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		EventViewComponent create(@BindsInstance @Named("owner") Stage owner);
	}
}
