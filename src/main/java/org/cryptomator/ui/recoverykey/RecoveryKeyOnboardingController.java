package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ResourceBundle;

import static org.cryptomator.common.recovery.RecoveryActionType.RESTORE_ALL;
import static org.cryptomator.common.recovery.RecoveryActionType.RESTORE_VAULT_CONFIG;

@RecoveryKeyScoped
public class RecoveryKeyOnboardingController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final Lazy<Scene> recoverykeyRecoverScene;
	private final Lazy<Scene> recoverykeyExpertSettingsScene;
	private final ObjectProperty<RecoveryActionType> recoverType;
	private final ResourceBundle resourceBundle;

	public Label titleLabel;
	public Label messageLabel;
	public Label pleaseConfirm;
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
	@FXML
	private ToggleGroup methodToggleGroup = new ToggleGroup();
	@FXML
	private HBox hBox;

	@Inject
	public RecoveryKeyOnboardingController(@RecoveryKeyWindow Stage window, //
										   @RecoveryKeyWindow Vault vault, //
										   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene, //
										   @FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> recoverykeyExpertSettingsScene, //
										   @Named("recoverType") ObjectProperty<RecoveryActionType> recoverType, //
										   ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.recoverykeyRecoverScene = recoverykeyRecoverScene;
		this.recoverykeyExpertSettingsScene = recoverykeyExpertSettingsScene;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		recoveryKeyRadio.setToggleGroup(methodToggleGroup);
		passwordRadio.setToggleGroup(methodToggleGroup);

		BooleanBinding showMethodSelection = Bindings.createBooleanBinding(
				() -> recoverType.get() == RecoveryActionType.RESTORE_VAULT_CONFIG, recoverType);
		chooseMethodeBox.visibleProperty().bind(showMethodSelection);
		chooseMethodeBox.managedProperty().bind(showMethodSelection);

		nextButton.disableProperty().bind(
				affirmationBox.selectedProperty().not()
						.or(methodToggleGroup.selectedToggleProperty().isNull().and(showMethodSelection))
		);

		switch (recoverType.get()) {
			case RESTORE_MASTERKEY -> {
				window.setTitle(resourceBundle.getString("recover.recoverMasterkey.title"));
				messageLabel.setVisible(false);
				messageLabel.setManaged(false);
				pleaseConfirm.setText(resourceBundle.getString("recover.onBoarding.pleaseConfirm"));
			}
			case RESTORE_ALL -> {
				window.setTitle(resourceBundle.getString("recover.recoverVaultConfig.title"));
				messageLabel.setVisible(true);
				messageLabel.setManaged(true);
				pleaseConfirm.setText(resourceBundle.getString("recover.onBoarding.otherwisePleaseConfirm"));
			}
			case RESTORE_VAULT_CONFIG -> {
				window.setTitle(resourceBundle.getString("recover.recoverVaultConfig.title"));
				messageLabel.setVisible(false);
				messageLabel.setManaged(false);
				pleaseConfirm.setText(resourceBundle.getString("recover.onBoarding.pleaseConfirm"));
			}
			default -> window.setTitle("");
		}

		if (vault.getState() == VaultState.Value.ALL_MISSING) {
			messageLabel.setText(resourceBundle.getString("recover.onBoarding.allMissing.intro"));
		} else {
			messageLabel.setText(resourceBundle.getString("recover.onBoarding.intro"));
		}

		titleLabel.textProperty().bind(Bindings.createStringBinding(() ->
				recoverType.get() == RecoveryActionType.RESTORE_MASTERKEY
						? resourceBundle.getString("recover.recoverMasterkey.title")
						: resourceBundle.getString("recover.recoverVaultConfig.title"), recoverType));

		BooleanBinding isRestoreMasterkey = Bindings.createBooleanBinding(
				() -> recoverType.get() == RecoveryActionType.RESTORE_MASTERKEY, recoverType);
		hBox.minHeightProperty().bind(Bindings.when(isRestoreMasterkey).then(206.0).otherwise(Region.USE_COMPUTED_SIZE));

		secondTextDesc.textProperty().bind(Bindings.createStringBinding(() -> {
			RecoveryActionType type = recoverType.get();
			Toggle sel = methodToggleGroup.getSelectedToggle();
			return switch (type) {
				case RESTORE_VAULT_CONFIG -> resourceBundle.getString(sel == passwordRadio
						? "recover.onBoarding.intro.password"
						: "recover.onBoarding.intro.recoveryKey");
				case RESTORE_MASTERKEY -> resourceBundle.getString("recover.onBoarding.intro.masterkey.recoveryKey");
				case RESTORE_ALL       -> resourceBundle.getString("recover.onBoarding.intro.recoveryKey");
				default                -> "";
			};
		}, recoverType, methodToggleGroup.selectedToggleProperty()));

		showMethodSelection.addListener((_, _, nowShown) -> {
			if (nowShown && methodToggleGroup.getSelectedToggle() == null) {
				methodToggleGroup.selectToggle(recoveryKeyRadio);
			}
		});
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
			default -> window.setScene(recoverykeyRecoverScene.get()); // Fallback
		}
		window.centerOnScreen();
	}

}
