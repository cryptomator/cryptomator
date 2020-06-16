package org.cryptomator.ui.unlock;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.common.WeakBindings;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@UnlockScoped
public class UnlockController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private final Stage window;
	private final Vault vault;
	private final AtomicReference<char[]> password;
	private final AtomicBoolean savePassword;
	private final Optional<char[]> savedPassword;
	private final UserInteractionLock<UnlockModule.PasswordEntry> passwordEntryLock;
	private final ForgetPasswordComponent.Builder forgetPassword;
	private final Optional<KeychainManager> keychain;
	private final ObjectBinding<ContentDisplay> unlockButtonContentDisplay;
	private final BooleanBinding userInteractionDisabled;
	private final BooleanProperty unlockButtonDisabled;
	private final StringBinding vaultName;

	public NiceSecurePasswordField passwordField;
	public CheckBox savePasswordCheckbox;
	public ImageView face;
	public ImageView leftArm;
	public ImageView rightArm;
	public ImageView legs;
	public ImageView body;
	public Animation unlockAnimation;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow Vault vault, AtomicReference<char[]> password, @Named("savePassword") AtomicBoolean savePassword, @Named("savedPassword") Optional<char[]> savedPassword, UserInteractionLock<UnlockModule.PasswordEntry> passwordEntryLock, ForgetPasswordComponent.Builder forgetPassword, Optional<KeychainManager> keychain) {
		this.window = window;
		this.vault = vault;
		this.password = password;
		this.savePassword = savePassword;
		this.savedPassword = savedPassword;
		this.passwordEntryLock = passwordEntryLock;
		this.forgetPassword = forgetPassword;
		this.keychain = keychain;
		this.unlockButtonContentDisplay = Bindings.createObjectBinding(this::getUnlockButtonContentDisplay, passwordEntryLock.awaitingInteraction());
		this.userInteractionDisabled = passwordEntryLock.awaitingInteraction().not();
		this.unlockButtonDisabled = new SimpleBooleanProperty();
		this.vaultName = WeakBindings.bindString(vault.displayableNameProperty());
		this.window.setOnCloseRequest(windowEvent -> cancel());
	}

	@FXML
	public void initialize() {
		savePasswordCheckbox.setSelected(savedPassword.isPresent());
		if (password.get() != null) {
			passwordField.setPassword(password.get());
		}
		unlockButtonDisabled.bind(userInteractionDisabled.or(passwordField.textProperty().isEmpty()));
		
		var leftArmTranslation = new Translate(24, 0);
		var leftArmRotation = new Rotate(60, 16, 30, 0);
		var leftArmRetracted = new KeyValue(leftArmTranslation.xProperty(), 24);
		var leftArmExtended = new KeyValue(leftArmTranslation.xProperty(), 0.0);
		var leftArmHorizontal = new KeyValue(leftArmRotation.angleProperty(), 60, Interpolator.EASE_OUT);
		var leftArmHanging = new KeyValue(leftArmRotation.angleProperty(), 0);
		leftArm.getTransforms().setAll(leftArmTranslation, leftArmRotation);

		var rightArmTranslation = new Translate(-24, 0);
		var rightArmRotation = new Rotate(60, 48, 30, 0);
		var rightArmRetracted = new KeyValue(rightArmTranslation.xProperty(), -24);
		var rightArmExtended = new KeyValue(rightArmTranslation.xProperty(), 0.0);
		var rightArmHorizontal = new KeyValue(rightArmRotation.angleProperty(), -60);
		var rightArmHanging = new KeyValue(rightArmRotation.angleProperty(), 0, Interpolator.EASE_OUT);
		rightArm.getTransforms().setAll(rightArmTranslation, rightArmRotation);

		var legsRetractedY = new KeyValue(legs.scaleYProperty(), 0);
		var legsExtendedY = new KeyValue(legs.scaleYProperty(), 1, Interpolator.EASE_OUT);
		var legsRetractedX = new KeyValue(legs.scaleXProperty(), 0);
		var legsExtendedX = new KeyValue(legs.scaleXProperty(), 1, Interpolator.EASE_OUT);
		legs.setScaleY(0);
		legs.setScaleX(0);

		var faceHidden = new KeyValue(face.opacityProperty(), 0.0);
		var faceVisible = new KeyValue(face.opacityProperty(), 1.0, Interpolator.LINEAR);
		face.setOpacity(0);

		unlockAnimation = new Timeline(
				new KeyFrame(Duration.ZERO, leftArmRetracted, leftArmHorizontal, rightArmRetracted, rightArmHorizontal, legsRetractedY, legsRetractedX, faceHidden),
				new KeyFrame(Duration.millis(200), leftArmExtended, leftArmHorizontal, rightArmRetracted, rightArmHorizontal),
				new KeyFrame(Duration.millis(400), leftArmExtended, leftArmHanging, rightArmExtended, rightArmHorizontal),
				new KeyFrame(Duration.millis(600), leftArmExtended, leftArmHanging, rightArmExtended, rightArmHanging),
				new KeyFrame(Duration.millis(800), legsExtendedY, legsExtendedX, faceHidden),
				new KeyFrame(Duration.millis(1000), faceVisible)
		);
		
		passwordEntryLock.awaitingInteraction().addListener(observable -> stopUnlockAnimation());
	}


	@FXML
	public void cancel() {
		LOG.debug("Unlock canceled by user.");
		window.close();
		passwordEntryLock.interacted(UnlockModule.PasswordEntry.CANCELED);
	}


	@FXML
	public void unlock() {
		LOG.trace("UnlockController.unlock()");
		CharSequence pwFieldContents = passwordField.getCharacters();
		char[] newPw = new char[pwFieldContents.length()];
		for (int i = 0; i < pwFieldContents.length(); i++) {
			newPw[i] = pwFieldContents.charAt(i);
		}
		char[] oldPw = password.getAndSet(newPw);
		if (oldPw != null) {
			Arrays.fill(oldPw, ' ');
		}
		passwordEntryLock.interacted(UnlockModule.PasswordEntry.PASSWORD_ENTERED);
		startUnlockAnimation();
	}
	
	private void startUnlockAnimation() {
		leftArm.setVisible(true);
		rightArm.setVisible(true);
		legs.setVisible(true);
		face.setVisible(true);
		unlockAnimation.playFromStart();
	}

	private void stopUnlockAnimation() {
		unlockAnimation.stop();
		leftArm.setVisible(false);
		rightArm.setVisible(false);
		legs.setVisible(false);
		face.setVisible(false);
	}

	/* Save Password */

	@FXML
	private void didClickSavePasswordCheckbox() {
		savePassword.set(savePasswordCheckbox.isSelected());
		if (!savePasswordCheckbox.isSelected() && savedPassword.isPresent()) {
			forgetPassword.vault(vault).owner(window).build().showForgetPassword().thenAccept(forgotten -> savePasswordCheckbox.setSelected(!forgotten));
		}
	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName.get();
	}
	
	public StringBinding vaultNameProperty() {
		return vaultName;
	}

	public ObjectBinding<ContentDisplay> unlockButtonContentDisplayProperty() {
		return unlockButtonContentDisplay;
	}

	public ContentDisplay getUnlockButtonContentDisplay() {
		return passwordEntryLock.awaitingInteraction().get() ? ContentDisplay.TEXT_ONLY : ContentDisplay.LEFT;
	}

	public BooleanBinding userInteractionDisabledProperty() {
		return userInteractionDisabled;
	}

	public boolean isUserInteractionDisabled() {
		return userInteractionDisabled.get();
	}

	public ReadOnlyBooleanProperty unlockButtonDisabledProperty() {
		return unlockButtonDisabled;
	}

	public boolean isUnlockButtonDisabled() {
		return unlockButtonDisabled.get();
	}

	public boolean isKeychainAccessAvailable() {
		return keychain.isPresent();
	}
}
