package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ResourceBundle;

import static org.cryptomator.common.recovery.RecoveryActionType.RESTORE_ALL;
import static org.cryptomator.common.recovery.RecoveryActionType.RESTORE_VAULT_CONFIG;

@RecoveryKeyScoped
public class RecoveryKeyOnboardingController implements FxController {

	private final Stage window;
	private final Lazy<Scene> recoverykeyRecoverScene;
	private final Lazy<Scene> recoverykeyExpertSettingsScene;
	private final ObjectProperty<RecoveryActionType> recoverType;
	private final ResourceBundle resourceBundle;

	public Label titleLabel;
	public Label messageLabel;
	public Label secondTextDesc;

	@FXML
	private CheckBox affirmationBox;
	@FXML
	private RadioButton recoveryKeyRadio;
	@FXML
	private RadioButton passwordRadio;
	@FXML
	private Button nextButton;
	@FXML
	private VBox chooseMethodeBox;
	private final ToggleGroup methodToggleGroup = new ToggleGroup();
	private final BooleanProperty showThirdText = new SimpleBooleanProperty(true);

	@Inject
	public RecoveryKeyOnboardingController(@RecoveryKeyWindow Stage window, //
										   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene, //
										   @FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> recoverykeyExpertSettingsScene, //
										   @Named("recoverType") ObjectProperty<RecoveryActionType> recoverType, //
										   ResourceBundle resourceBundle) {
		this.window = window;
		window.setTitle(resourceBundle.getString("recoveryKey.recoverVaultConfig.title"));

		this.recoverykeyRecoverScene = recoverykeyRecoverScene;
		this.recoverykeyExpertSettingsScene = recoverykeyExpertSettingsScene;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {

		recoveryKeyRadio.setToggleGroup(methodToggleGroup);
		passwordRadio.setToggleGroup(methodToggleGroup);

		boolean showMethodSelection = (recoverType.get() == RecoveryActionType.RESTORE_VAULT_CONFIG);
		chooseMethodeBox.setVisible(showMethodSelection);
		chooseMethodeBox.setManaged(showMethodSelection);

		nextButton.disableProperty().bind( //
				affirmationBox.selectedProperty().not() //
						.or(methodToggleGroup.selectedToggleProperty().isNull() //
								.and(showMethodSelectionProperty())));

		switch (recoverType.get()) {
			case RESTORE_VAULT_CONFIG -> {
				window.setTitle(resourceBundle.getString("recoveryKey.recoverVaultConfig.title"));
				messageLabel.setText(resourceBundle.getString("recoveryKey.recover.onBoarding.readThis"));
				secondTextDesc.setText(resourceBundle.getString("recoveryKey.recover.onBoarding.recoverVaultConfig.intro2"));
				showThirdText.set(false);
			}
			case RESTORE_MASTERKEY -> {
				window.setTitle(resourceBundle.getString("recoveryKey.recoverMasterkey.title"));
				messageLabel.setText(resourceBundle.getString("recoveryKey.recover.onBoarding.readThis"));
				titleLabel.setText(resourceBundle.getString("recoveryKey.recoverMasterkey.title"));
				secondTextDesc.setText(resourceBundle.getString("recoveryKey.recover.onBoarding.recoverMasterkey.intro2"));
				showThirdText.set(false);
			}
		}
	}

	private BooleanProperty showMethodSelectionProperty() {
		return new SimpleBooleanProperty(recoverType.get() == RecoveryActionType.RESTORE_VAULT_CONFIG);
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void next() {
		switch (recoverType.get()) {
			case RESTORE_VAULT_CONFIG, RESTORE_ALL -> {
				Object selectedToggle = methodToggleGroup.getSelectedToggle();
				if (selectedToggle == recoveryKeyRadio) {
					recoverType.set(RESTORE_ALL);
					window.setScene(recoverykeyRecoverScene.get());
				} else if (selectedToggle == passwordRadio) {
					recoverType.set(RESTORE_VAULT_CONFIG);
					window.setScene(recoverykeyExpertSettingsScene.get());
				} else {
					window.setScene(recoverykeyRecoverScene.get());
				}
			}
			case RESTORE_MASTERKEY -> window.setScene(recoverykeyRecoverScene.get());
		}
		window.centerOnScreen();
	}

	public BooleanProperty showThirdTextProperty() {
		return showThirdText;
	}

	public boolean getShowThirdText() {
		return showThirdText.get();
	}

}

