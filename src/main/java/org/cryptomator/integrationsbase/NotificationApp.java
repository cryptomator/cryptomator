package org.cryptomator.integrationsbase;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.BuilderFactory;
import java.io.IOException;
import java.util.ResourceBundle;


public class NotificationApp extends Application {


	private static final KeyCodeCombination ALT_F4 = new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN);
	private static final KeyCodeCombination SHORTCUT_W = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);

	@Override
	public void start(Stage stage) throws IOException {
		var args = getParameters();
		String javaVersion = System.getProperty("java.version");
		var url = getClass().getResource("/fxml/notification_window.fxml");
		var root = FXMLLoader.<AnchorPane>load(url, ResourceBundle.getBundle("i18n.strings"));
		var scene = new Scene(root);
		stage.setScene(scene);
		setupDefaultAccelerators(scene, stage);
		stage.show();
		stage.requestFocus();
	}

	public static void main(String[] args) {
		//assert args.length >= 2;
		launch(args);
	}

	private void setupDefaultAccelerators(Scene scene, Stage stage) {
		if (SystemUtils.IS_OS_WINDOWS) {
			scene.getAccelerators().put(ALT_F4, stage::close);
		} else {
			scene.getAccelerators().put(SHORTCUT_W, stage::close);
		}
	}
}
