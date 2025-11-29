package org.cryptomator.ui.notification;

import dagger.BindsInstance;
import dagger.Subcomponent;

import javafx.stage.Stage;

@NotificationScoped
@Subcomponent(modules = {NotificationModule.class})
public interface NotificationComponent {

	@NotificationWindow
	Stage notificationWindow();

	default Stage showNotification(){
		var window = notificationWindow();
		window.show();
		return window;
	}

	@Subcomponent.Factory
	interface Factory {
		NotificationComponent create(@BindsInstance Runnable action);
	}

}
