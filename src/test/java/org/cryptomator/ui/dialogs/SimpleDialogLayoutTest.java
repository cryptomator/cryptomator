package org.cryptomator.ui.dialogs;

import org.cryptomator.JavaFXUtil;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class SimpleDialogLayoutTest {

	@BeforeAll
	static void initJavaFx() throws InterruptedException {
		Assumptions.assumeTrue(JavaFXUtil.startPlatform());
	}

	@Test
	@DisplayName("long button labels are fully visible")
	void longButtonLabelsAreNotCropped() throws InterruptedException {
		runOnFxThread(() -> assertButtonLabelsAreNotCropped("Rimuovi Cassaforte", "Annulla"));
	}

	@Test
	@DisplayName("short button labels keep the compact dialog width")
	void shortButtonLabelsKeepCompactWidth() throws InterruptedException {
		runOnFxThread(() -> {
			var root = layoutDialog("Remove", "Cancel");
			Assertions.assertTrue(root.getWidth() >= 399.0 && root.getWidth() <= 420.0, //
					() -> "Compact dialog should stay near 400px, was " + root.getWidth());
		});
	}

	private static void runOnFxThread(Runnable assertion) throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<Throwable> error = new AtomicReference<>();

		Platform.runLater(() -> {
			try {
				assertion.run();
			} catch (Throwable t) {
				error.set(t);
			} finally {
				done.countDown();
			}
		});

		Assertions.assertTrue(done.await(15, TimeUnit.SECONDS), "JavaFX layout timed out");
		if (error.get() != null) {
			Assertions.fail(error.get());
		}
	}

	private static void assertButtonLabelsAreNotCropped(String okLabel, String cancelLabel) {
		var controller = new SimpleDialogController( //
				"Remove vault?", //
				"This will only make Cryptomator forget about this vault. You can add it again.", //
				FontAwesome5Icon.QUESTION, //
				okLabel, //
				cancelLabel, //
				() -> {}, //
				() -> {});
		var factory = FxmlLoaderFactory.forController(controller, Scene::new, ResourceBundle.getBundle("i18n.strings"));
		Scene scene = factory.createScene(FxmlFile.SIMPLE_DIALOG);
		scene.getStylesheets().add(SimpleDialogLayoutTest.class.getResource("/css/light_theme.css").toExternalForm());
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setMinWidth(400);

		Region root = (Region) scene.getRoot();
		root.applyCss();
		ButtonBar buttonBar = (ButtonBar) root.lookup(".button-bar");
		Assertions.assertNotNull(buttonBar, "simple_dialog.fxml should contain a ButtonBar");
		var unconstrainedPrefs = new LinkedHashMap<Button, Double>();
		for (var node : buttonBar.getButtons()) {
			if (node instanceof Button button && button.isVisible() && button.isManaged()) {
				unconstrainedPrefs.put(button, button.prefWidth(-1));
			}
		}

		SimpleDialog.fitDialogToButtonLabels(stage);
		root.autosize();
		root.layout();

		unconstrainedPrefs.forEach((button, prefWidth) -> Assertions.assertTrue(button.getWidth() + 0.5 >= prefWidth, //
				() -> "Button '%s' was cropped: width=%s unconstrainedPref=%s".formatted(button.getText(), button.getWidth(), prefWidth)));
		stage.close();
	}

	private static Region layoutDialog(String okLabel, String cancelLabel) {
		var controller = new SimpleDialogController( //
				"Remove vault?", //
				"This will only make Cryptomator forget about this vault. You can add it again.", //
				FontAwesome5Icon.QUESTION, //
				okLabel, //
				cancelLabel, //
				() -> {}, //
				() -> {});
		var factory = FxmlLoaderFactory.forController(controller, Scene::new, ResourceBundle.getBundle("i18n.strings"));
		Scene scene = factory.createScene(FxmlFile.SIMPLE_DIALOG);
		scene.getStylesheets().add(SimpleDialogLayoutTest.class.getResource("/css/light_theme.css").toExternalForm());
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setMinWidth(400);
		SimpleDialog.fitDialogToButtonLabels(stage);
		Region root = (Region) scene.getRoot();
		root.autosize();
		root.layout();
		stage.close();
		return root;
	}

}
