package org.cryptomator.ui.notification;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageInitializer;
import org.cryptomator.ui.common.SystemBarUtil;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class NotificationModule {

	@Provides
	@NotificationWindow
	@NotificationScoped
	static Stage provideStage(StageInitializer initializer) {
		Stage stage = new Stage(StageStyle.TRANSPARENT);
		stage.setTitle("Filesystem notification"); //TODO: translate
		stage.setResizable(false);
		stage.initModality(Modality.NONE);
		stage.setAlwaysOnTop(true);
		initializer.accept(stage);
		stage.setOnShown(_ -> placeWindow(stage));
		return stage;
	}

	/**
	 * Places the notification window on the screen according to some heuristic based on operating system and system bar placement.
	 * <p>
	 * On macOS, the window is placed in the top-right corner of the primary screen, following platform conventions.
	 * On other operating systems, the window placement depends on the location of the system bar:
	 * <ul>
	 *   <li>If the system bar is at the top, the window is centered horizontally at the top of the screen.</li>
	 *   <li>Otherwise (e.g., system bar at the bottom or elsewhere), the window is placed in the bottom-right corner.</li>
	 * </ul>
	 * <p>
	 * The method uses the visual bounds of the primary screen to avoid overlapping with system UI elements.
	 * Assumes the window size has already been set before calling this method.
	 *
	 * @param window the Stage representing the notification window to be placed
	 */
	static void placeWindow(Stage window) {
		var screen = Screen.getPrimary();
		var vBounds = screen.getVisualBounds();
		if (SystemUtils.IS_OS_MAC) { //place to right top
			window.setX(vBounds.getMaxX() - window.getWidth());
			window.setY(vBounds.getMinY());
		} else {
			switch (SystemBarUtil.getPlacementOfSystembar(screen)) {
				case TOP -> { //place to middle top
					window.setX(vBounds.getMinX() + (vBounds.getWidth() - window.getWidth()) / 2.0);
					window.setY(vBounds.getMinY());
				}
				default -> { //place to right bottom
					window.setX(vBounds.getMaxX() - window.getWidth());
					window.setY(vBounds.getMaxY() - window.getHeight());
				}
			}
		}
	}

	// javafx setup

	@Provides
	@FxmlScene(FxmlFile.NOTIFICATION)
	@NotificationScoped
	static Scene provideNotificationScene(@NotificationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.NOTIFICATION);
	}

	@Provides
	@NotificationScoped
	@NotificationWindow
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Binds
	@IntoMap
	@FxControllerKey(NotificationController.class)
	abstract FxController bindNotificationController(NotificationController controller);

}
