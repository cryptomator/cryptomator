package org.cryptomator.ui.common;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Performs common setup for all stages
 */
@FxApplicationScoped
public class StageInitializer implements Consumer<Stage> {

	private final List<Image> windowIcons;

	@Inject
	public StageInitializer() {
		this.windowIcons = SystemUtils.IS_OS_MAC ? List.of() : List.of( //
				new Image(StageInitializer.class.getResource("/img/window_icon_32.png").toString()), //
				new Image(StageInitializer.class.getResource("/img/window_icon_512.png").toString()) //
		);
	}

	@Override
	public void accept(Stage stage) {
		stage.getIcons().setAll(windowIcons);
	}
}
