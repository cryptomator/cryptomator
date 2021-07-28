package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import org.cryptomator.common.Environment;
import org.cryptomator.cryptolib.common.Destroyables;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.NewPasswordController;
import org.cryptomator.ui.common.UserInteractionLock;
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
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class P12CreateController implements FxController  {

	private static final Logger LOG = LoggerFactory.getLogger(P12LoadController.class);

	private final Stage window;
	private final Environment env;
	private final AtomicReference<KeyPair> keyPairRef;
	private final UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock;
	private final BooleanProperty userInteractionDisabled = new SimpleBooleanProperty();
	private final ObjectBinding<ContentDisplay> unlockButtonContentDisplay = Bindings.createObjectBinding(this::getUnlockButtonContentDisplay, userInteractionDisabled);
	private final BooleanProperty readyToCreate = new SimpleBooleanProperty();

	public NewPasswordController newPasswordController;


	@Inject
	public P12CreateController(@KeyLoading Stage window, Environment env, AtomicReference<KeyPair> keyPairRef, UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock) {
		this.window = window;
		this.env = env;
		this.keyPairRef = keyPairRef;
		this.p12LoadingLock = p12LoadingLock;
	}

	@FXML
	public void initialize() {
		readyToCreate.bind(newPasswordController.goodPasswordProperty());
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void create() {
		Preconditions.checkState(newPasswordController.goodPasswordProperty().get());
		char[] pw = newPasswordController.passwordField.copyChars();
		try {
			Path p12File = env.getP12Path().findFirst().orElseThrow(IllegalStateException::new);
			var keyPair = P12AccessHelper.createNew(p12File, pw);
			setKeyPair(keyPair);
			LOG.debug("Created .p12 file {}", p12File);
			p12LoadingLock.interacted(HubKeyLoadingModule.P12KeyLoading.CREATED);
			window.close();
		} catch (IOException e) {
			LOG.error("Failed to load .p12 file.", e);
			// TODO
		} finally {
			Arrays.fill(pw, '\0');
			newPasswordController.passwordField.wipe();
			newPasswordController.reenterField.wipe();
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

	public BooleanProperty readyToCreateProperty() {
		return readyToCreate;
	}

	public boolean isReadyToCreate() {
		return readyToCreate.get();
	}

}
