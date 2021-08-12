package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
import org.cryptomator.common.Environment;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.Destroyables;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class P12LoadController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(P12LoadController.class);

	private final Stage window;
	private final Environment env;
	private final AtomicReference<KeyPair> keyPairRef;
	private final Lazy<Scene> authFlowScene;
	private final BooleanProperty userInteractionDisabled = new SimpleBooleanProperty();
	private final ObjectBinding<ContentDisplay> unlockButtonContentDisplay = Bindings.createObjectBinding(this::getUnlockButtonContentDisplay, userInteractionDisabled);

	public NiceSecurePasswordField passwordField;

	@Inject
	public P12LoadController(@KeyLoading Stage window, Environment env, AtomicReference<KeyPair> keyPairRef, @FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene) {
		this.window = window;
		this.env = env;
		this.keyPairRef = keyPairRef;
		this.authFlowScene = authFlowScene;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void initialize() {
		passwordField.requestFocus();
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		passwordField.wipe();
	}

	@FXML
	public void load() {
		char[] pw = passwordField.copyChars();
		try {
			Path p12File = env.getP12Path().filter(Files::isRegularFile).findFirst().orElseThrow(IllegalStateException::new);
			var keyPair = P12AccessHelper.loadExisting(p12File, pw);
			setKeyPair(keyPair);
			LOG.debug("Loaded .p12 file {}", p12File);
			window.setScene(authFlowScene.get());
		} catch (InvalidPassphraseException e) {
			LOG.warn("Invalid passphrase entered for .p12 file");
			Animations.createShakeWindowAnimation(window).playFromStart();
			// TODO
		} catch (IOException e) {
			LOG.error("Failed to load .p12 file.", e);
			// TODO
		} finally {
			Arrays.fill(pw, '\0');
		}
	}

	private void setKeyPair(KeyPair keyPair) {
		var oldKeyPair = keyPairRef.getAndSet(keyPair);
		if (oldKeyPair != null) {
			Destroyables.destroySilently(oldKeyPair.getPrivate());
		}
	}

	/* Getter/Setter */

	public BooleanExpression userInteractionDisabledProperty() {
		return userInteractionDisabled;
	}

	public boolean isUserInteractionDisabled() {
		return userInteractionDisabled.get();
	}

	public ObjectExpression<ContentDisplay> unlockButtonContentDisplayProperty() {
		return unlockButtonContentDisplay;
	}

	public ContentDisplay getUnlockButtonContentDisplay() {
		return userInteractionDisabled.get() ? ContentDisplay.LEFT : ContentDisplay.TEXT_ONLY;
	}
}
