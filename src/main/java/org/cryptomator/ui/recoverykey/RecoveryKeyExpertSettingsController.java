package org.cryptomator.ui.recoverykey;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import dagger.Lazy;
import org.cryptomator.ui.addvaultwizard.CreateNewVaultExpertSettingsController;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.NumericTextField;

@RecoveryKeyScoped
public class RecoveryKeyExpertSettingsController implements FxController {

	public static final int MAX_SHORTENING_THRESHOLD = 220;
	public static final int MIN_SHORTENING_THRESHOLD = 36;
	private static final String DOCS_NAME_SHORTENING_URL = "https://docs.cryptomator.org/security/architecture/#name-shortening";

	private final Stage window;
	private final Lazy<Application> application;
	private final Lazy<Scene> resetPasswordScene;
	private final Lazy<Scene> recoverScene;

	public CheckBox expertSettingsCheckBox;
	public NumericTextField shorteningThresholdTextField;
	private final IntegerProperty shorteningThreshold;
	private final BooleanBinding validShorteningThreshold;


	@Inject
	public RecoveryKeyExpertSettingsController(@RecoveryKeyWindow Stage window, //
											   Lazy<Application> application, //
											   @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
											   @FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene, //
											   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverScene) {
		this.window = window;
		this.application = application;
		this.resetPasswordScene = resetPasswordScene;
		this.recoverScene = recoverScene;
		this.shorteningThreshold = shorteningThreshold;
		this.validShorteningThreshold = Bindings.createBooleanBinding(this::isValidShorteningThreshold, shorteningThreshold);

	}

	@FXML
	public void initialize() {
		shorteningThresholdTextField.setPromptText(MIN_SHORTENING_THRESHOLD + "-" + MAX_SHORTENING_THRESHOLD);
		shorteningThresholdTextField.setText(Integer.toString(MAX_SHORTENING_THRESHOLD));
		shorteningThresholdTextField.textProperty().addListener((_, _, newValue) -> {
			try {
				int intValue = Integer.parseInt(newValue);
				shorteningThreshold.set(intValue);
			} catch (NumberFormatException e) {
				shorteningThreshold.set(0); //the value is set to 0 to ensure that an invalid value assignment is detected during a NumberFormatException
			}
		});
	}

	@FXML
	public void toggleUseExpertSettings() {
		if (!expertSettingsCheckBox.isSelected()) {
			shorteningThresholdTextField.setText(Integer.toString(CreateNewVaultExpertSettingsController.MAX_SHORTENING_THRESHOLD));
		}
	}

	public void openDocs() {
		application.get().getHostServices().showDocument(DOCS_NAME_SHORTENING_URL);
	}

	public BooleanBinding validShorteningThresholdProperty() {
		return validShorteningThreshold;
	}

	public boolean isValidShorteningThreshold() {
		var value = shorteningThreshold.get();
		return value >= MIN_SHORTENING_THRESHOLD && value <= MAX_SHORTENING_THRESHOLD;
	}

	@FXML
	public void back() {
		window.setScene(recoverScene.get());
	}

	@FXML
	public void next() {
		window.setScene(resetPasswordScene.get());
	}
}
