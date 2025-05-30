package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.common.Nullable;
import org.cryptomator.common.Passphrase;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.WeakBindings;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.forgetpassword.ForgetPasswordComponent;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@PassphraseEntryScoped
public class PassphraseEntryController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(PassphraseEntryController.class);

	private final Stage window;
	private final Vault vault;
	private final CompletableFuture<PassphraseEntryResult> result;
	private final Passphrase savedPassword;
	private final ForgetPasswordComponent.Builder forgetPassword;
	private final KeychainManager keychain;
	private final StringBinding vaultName;
	private final ExecutorService backgroundExecutorService;
	private final BooleanProperty unlockInProgress = new SimpleBooleanProperty();
	private final ObjectBinding<ContentDisplay> unlockButtonContentDisplay = Bindings.when(unlockInProgress).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
	private final BooleanProperty unlockButtonDisabled = new SimpleBooleanProperty();

	/* FXML */
	public NiceSecurePasswordField passwordField;
	public CheckBox savePasswordCheckbox;
	public ImageView face;
	public ImageView leftArm;
	public ImageView rightArm;
	public ImageView legs;
	public ImageView body;
	public Animation unlockAnimation;

	@Inject
	public PassphraseEntryController(@KeyLoading Stage window, @KeyLoading Vault vault, CompletableFuture<PassphraseEntryResult> result, @Nullable @Named("savedPassword") Passphrase savedPassword, ForgetPasswordComponent.Builder forgetPassword, KeychainManager keychain, ExecutorService backgroundExecutorService) {
		this.window = window;
		this.vault = vault;
		this.result = result;
		this.savedPassword = savedPassword;
		this.forgetPassword = forgetPassword;
		this.keychain = keychain;
		this.vaultName = WeakBindings.bindString(vault.displayNameProperty());
		this.backgroundExecutorService = backgroundExecutorService;
		window.setOnHiding(this::windowClosed);
	}

	@FXML
	public void initialize() {
		if (savedPassword != null) {
			savePasswordCheckbox.setSelected(true);
			passwordField.setPassword(savedPassword);
		}
		unlockButtonDisabled.bind(unlockInProgress.or(passwordField.textProperty().isEmpty()));

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

		unlockAnimation = new Timeline( //
				new KeyFrame(Duration.ZERO, leftArmRetracted, leftArmHorizontal, rightArmRetracted, rightArmHorizontal, legsRetractedY, legsRetractedX, faceHidden), //
				new KeyFrame(Duration.millis(200), leftArmExtended, leftArmHorizontal, rightArmRetracted, rightArmHorizontal), //
				new KeyFrame(Duration.millis(400), leftArmExtended, leftArmHanging, rightArmExtended, rightArmHorizontal), //
				new KeyFrame(Duration.millis(600), leftArmExtended, leftArmHanging, rightArmExtended, rightArmHanging), //
				new KeyFrame(Duration.millis(800), legsExtendedY, legsExtendedX, faceHidden), //
				new KeyFrame(Duration.millis(1000), faceVisible) //
		);
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		if(!result.isDone()) {
			result.cancel(true);
			LOG.debug("Unlock canceled by user.");
		}
		if( passwordField != null) {
			passwordField.getCharacters().destroy();
		}

	}

	@FXML
	public void unlock() {
		LOG.trace("UnlockController.unlock()");
		unlockInProgress.set(true);
		CharSequence pwFieldContents = passwordField.getCharacters();
		Passphrase pw = Passphrase.copyOf(pwFieldContents);
		result.completeAsync(() -> new PassphraseEntryResult(pw, savePasswordCheckbox.isSelected()), backgroundExecutorService);
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
		if (!savePasswordCheckbox.isSelected() && savedPassword != null) {
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
		return unlockButtonContentDisplay.get();
	}

	public ReadOnlyBooleanProperty userInteractionDisabledProperty() {
		return unlockInProgress;
	}

	public boolean isUserInteractionDisabled() {
		return unlockInProgress.get();
	}

	public ReadOnlyBooleanProperty unlockButtonDisabledProperty() {
		return unlockButtonDisabled;
	}

	public boolean isUnlockButtonDisabled() {
		return unlockButtonDisabled.get();
	}

	public boolean isKeychainAccessAvailable() {
		return keychain.isSupported();
	}


}
