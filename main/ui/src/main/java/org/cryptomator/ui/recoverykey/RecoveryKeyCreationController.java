package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

@RecoveryKeyScoped
public class RecoveryKeyCreationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final Vault vault;
	private final ExecutorService executor;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final StringProperty recoveryKeyProperty;
	public NiceSecurePasswordField passwordField;

	@Inject
	public RecoveryKeyCreationController(@RecoveryKeyWindow Stage window, @FxmlScene(FxmlFile.RECOVERYKEY_DISPLAY) Lazy<Scene> successScene, @RecoveryKeyWindow Vault vault, RecoveryKeyFactory recoveryKeyFactory, ExecutorService executor, @RecoveryKeyWindow StringProperty recoveryKey) {
		this.window = window;
		this.successScene = successScene;
		this.vault = vault;
		this.executor = executor;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.recoveryKeyProperty = recoveryKey;
	}
	
	@FXML
	public void createRecoveryKey() {
		Tasks.create(() -> {
			return recoveryKeyFactory.createRecoveryKey(vault.getPath(), passwordField.getCharacters());
		}).onSuccess(result -> {
			recoveryKeyProperty.set(result);
			window.setScene(successScene.get());
		}).onError(IOException.class, e -> {
			LOG.error("Creation of recovery key failed.", e);
		}).onError(InvalidPassphraseException.class, e -> {
			shakeWindow();
		}).runOnce(executor);
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Animations */

	private void shakeWindow() {
		WritableValue<Double> writableWindowX = new WritableValue<>() {
			@Override
			public Double getValue() {
				return window.getX();
			}

			@Override
			public void setValue(Double value) {
				window.setX(value);
			}
		};
		Timeline timeline = new Timeline( //
				new KeyFrame(Duration.ZERO, new KeyValue(writableWindowX, window.getX())), //
				new KeyFrame(new Duration(100), new KeyValue(writableWindowX, window.getX() - 22.0)), //
				new KeyFrame(new Duration(200), new KeyValue(writableWindowX, window.getX() + 18.0)), //
				new KeyFrame(new Duration(300), new KeyValue(writableWindowX, window.getX() - 14.0)), //
				new KeyFrame(new Duration(400), new KeyValue(writableWindowX, window.getX() + 10.0)), //
				new KeyFrame(new Duration(500), new KeyValue(writableWindowX, window.getX() - 6.0)), //
				new KeyFrame(new Duration(600), new KeyValue(writableWindowX, window.getX() + 2.0)), //
				new KeyFrame(new Duration(700), new KeyValue(writableWindowX, window.getX())) //
		);
		timeline.play();
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}
