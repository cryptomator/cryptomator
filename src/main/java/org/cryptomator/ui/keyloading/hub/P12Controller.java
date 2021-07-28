package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import org.cryptomator.common.Environment;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.Destroyables;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.NewPasswordController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class P12Controller implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(P12Controller.class);

	private final Stage window;
	private final Environment env;
	private final UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock;

	@Inject
	public P12Controller(@KeyLoading Stage window, Environment env, AtomicReference<KeyPair> keyPairRef, UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock) {
		this.window = window;
		this.env = env;
		this.p12LoadingLock = p12LoadingLock;
		this.window.setOnHiding(this::windowClosed);
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (p12LoadingLock.awaitingInteraction().get()) {
			LOG.debug("P12 loading canceled by user.");
			p12LoadingLock.interacted(HubKeyLoadingModule.P12KeyLoading.CANCELED);
		}
	}

	/* Getter/Setter */

	public boolean isP12Present() {
		return env.getP12Path().anyMatch(Files::isRegularFile);
	}

}
