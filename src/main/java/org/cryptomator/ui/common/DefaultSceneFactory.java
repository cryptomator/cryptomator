package org.cryptomator.ui.common;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.function.Function;

@FxApplicationScoped
public class DefaultSceneFactory implements Function<Parent, Scene> {

	protected static final KeyCodeCombination ALT_F4 = new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN);
	protected static final KeyCodeCombination SHORTCUT_W = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);

	protected final Settings settings;

	@Inject
	public DefaultSceneFactory(Settings settings) {
		this.settings = settings;
	}

	@Override
	public Scene apply(Parent root) {
		Scene scene = new Scene(root);
		configureRoot(root);
		configureScene(scene);
		return scene;
	}

	protected void configureRoot(Parent root) {
		root.nodeOrientationProperty().bind(settings.userInterfaceOrientation);
	}

	protected void configureScene(Scene scene) {
		scene.windowProperty().addListener(observable -> {
			Window window = scene.getWindow();
			if (window instanceof Stage s) {
				setupDefaultAccelerators(scene, s);
			}
		});
	}

	protected void setupDefaultAccelerators(Scene scene, Stage stage) {
		if (SystemUtils.IS_OS_WINDOWS) {
			scene.getAccelerators().put(ALT_F4, stage::close);
		} else {
			scene.getAccelerators().put(SHORTCUT_W, stage::close);
		}
	}

}
