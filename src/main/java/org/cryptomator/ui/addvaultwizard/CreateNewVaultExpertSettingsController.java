package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.NumericTextField;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.nio.file.Path;

@AddVaultWizardScoped
public class CreateNewVaultExpertSettingsController implements FxController {

	public static final int MAX_SHORTENING_THRESHOLD = 220;
	public static final int MIN_SHORTENING_THRESHOLD = 36;
	private static final String DOCS_NAME_SHORTENING_URL = "https://docs.cryptomator.org/en/1.7/security/architecture/#name-shortening";

	private final Stage window;
	private final Lazy<Application> application;
	private final Lazy<Scene> chooseLocationScene;
	private final Lazy<Scene> choosePasswordScene;
	private final StringProperty vaultNameProperty;
	private final ObjectProperty<Path> vaultPathProperty;
	private final IntegerProperty shorteningThreshold;

	private final BooleanBinding validShorteningThreshold;

	//FXML
	public Label vaultNameLabel;
	public Label vaultPathLabel;
	public CheckBox expertSettingsCheckBox;
	public NumericTextField shorteningThresholdTextField;

	@Inject
	CreateNewVaultExpertSettingsController(@AddVaultWizardWindow Stage window, //
										   Lazy<Application> application, //
										   @FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION) Lazy<Scene> chooseLocationScene, //
										   @FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD) Lazy<Scene> choosePasswordScene, //
										   @Named("vaultName") StringProperty vaultName, //
										   ObjectProperty<Path> vaultPath, //
										   @Named("shorteningThreshold") IntegerProperty shorteningThreshold) {
		this.window = window;
		this.application = application;
		this.chooseLocationScene = chooseLocationScene;
		this.choosePasswordScene = choosePasswordScene;
		this.vaultNameProperty = vaultName;
		this.vaultPathProperty = vaultPath;
		this.shorteningThreshold = shorteningThreshold;
		this.validShorteningThreshold = Bindings.createBooleanBinding(this::isValidShorteningThreshold, shorteningThreshold);
	}

	@FXML
	public void initialize() {
		vaultNameLabel.textProperty().bind(vaultNameProperty);
		vaultPathLabel.textProperty().bind(vaultPathProperty.asString());
		shorteningThresholdTextField.setPromptText(MIN_SHORTENING_THRESHOLD + "-" + MAX_SHORTENING_THRESHOLD);
		shorteningThresholdTextField.setText(Integer.toString(MAX_SHORTENING_THRESHOLD));
		shorteningThresholdTextField.textProperty().addListener((observable, oldValue, newValue) -> {
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
			shorteningThresholdTextField.setText(Integer.toString(MAX_SHORTENING_THRESHOLD));
		}
	}

	@FXML
	public void back() {
		window.setScene(chooseLocationScene.get());
	}

	@FXML
	public void next() {
		window.setScene(choosePasswordScene.get());
	}

	public BooleanBinding validShorteningThresholdProperty() {
		return validShorteningThreshold;
	}

	public boolean isValidShorteningThreshold() {
		var value = shorteningThreshold.get();
		return value >= MIN_SHORTENING_THRESHOLD && value <= MAX_SHORTENING_THRESHOLD;
	}

	public void openDocs() {
		application.get().getHostServices().showDocument(DOCS_NAME_SHORTENING_URL);
	}

	public Path getVaultPath() {
		return vaultPathProperty.get();
	}

	public String getVaultName() {
		return vaultNameProperty.get();
	}
}