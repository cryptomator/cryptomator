package org.cryptomator.ui.notification;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@NotificationScoped
@Subcomponent(modules = {NotificationModule.class})
public interface NotificationComponent {

	@NotificationWindow
	Stage window();

	@FxmlScene(FxmlFile.NOTIFICATION)
	Lazy<Scene> scene();

	default Stage showNotification() {
		var window = window();
		window.setScene(scene().get());
		window.sizeToScene();
		window.show();
		window.requestFocus();
		return window;
	}

	@Subcomponent.Factory
	interface Factory {
		NotificationComponent create();
	}

}
