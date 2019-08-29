package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class CreateNewVaultPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CreateNewVaultPasswordController.class);

	private final Stage window;
	private final Lazy<Scene> chooseLocationScene;
	private final Lazy<Scene> successScene;
	private final StringProperty vaultName;
	private final ObjectProperty<Path> vaultPath;
	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> vault;
	private final VaultFactory vaultFactory;
	private final ResourceBundle resourceBundle;
	private final PasswordStrengthUtil strengthRater;
	private final ReadmeGenerator readmeGenerator;
	private final IntegerProperty passwordStrength;

	public Button finishButton;
	public SecPasswordField passwordField;
	public SecPasswordField reenterField;
	public Label passwordStrengthLabel;
	public HBox passwordMatchBox;
	public FontAwesome5IconView checkmark;
	public FontAwesome5IconView cross;
	public Label passwordMatchLabel;
	public CheckBox finalConfirmationCheckbox;

	@Inject
	CreateNewVaultPasswordController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION) Lazy<Scene> chooseLocationScene, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, StringProperty vaultName, ObjectProperty<Path> vaultPath, ObservableList<Vault> vaults, @AddVaultWizard ObjectProperty<Vault> vault, VaultFactory vaultFactory, ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater, ReadmeGenerator readmeGenerator) {
		this.window = window;
		this.chooseLocationScene = chooseLocationScene;
		this.successScene = successScene;
		this.vaultName = vaultName;
		this.vaultPath = vaultPath;
		this.vaults = vaults;
		this.vault = vault;
		this.vaultFactory = vaultFactory;
		this.resourceBundle = resourceBundle;
		this.strengthRater = strengthRater;
		this.readmeGenerator = readmeGenerator;
		this.passwordStrength = new SimpleIntegerProperty(-1);
	}

	@FXML
	public void initialize() {
		//binds the actual strength value to the rating of the password util
		passwordStrength.bind(Bindings.createIntegerBinding(() -> strengthRater.computeRate(passwordField.getCharacters().toString()), passwordField.textProperty()));
		//binding indicating if the passwords not match
		BooleanBinding passwordsMatch = Bindings.createBooleanBinding(() -> CharSequence.compare(passwordField.getCharacters(), reenterField.getCharacters()) == 0, passwordField.textProperty(), reenterField.textProperty());
		BooleanBinding reenterFieldNotEmpty = reenterField.textProperty().isNotEmpty();
		//disable the finish button when passwords do not match or one is empty
		finishButton.disableProperty().bind(reenterFieldNotEmpty.not().or(passwordsMatch.not()).or(finalConfirmationCheckbox.selectedProperty().not()));
		//make match indicator invisible when passwords do not match or one is empty
		passwordMatchBox.visibleProperty().bind(reenterFieldNotEmpty);
		checkmark.visibleProperty().bind(passwordsMatch.and(reenterFieldNotEmpty));
		checkmark.managedProperty().bind(checkmark.visibleProperty());
		cross.visibleProperty().bind(passwordsMatch.not().and(reenterFieldNotEmpty));
		cross.managedProperty().bind(cross.visibleProperty());
		passwordMatchLabel.textProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(resourceBundle.getString("addvaultwizard.new.passwordsMatch")).otherwise(resourceBundle.getString("addvaultwizard.new.passwordsDoNotMatch")));

		//bindsings for the password strength indicator
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	@FXML
	public void back() {
		window.setScene(chooseLocationScene.get());
	}

	@FXML
	public void next() {
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path().setValue(vaultPath.get());
		Vault newVault = vaultFactory.get(vaultSettings);
		try {
			Files.createDirectory(vaultSettings.path().get());
		} catch (FileAlreadyExistsException e) {
			// TODO show specific error screen
			LOG.error("", e);
		} catch (IOException e) {
			// TODO show generic error screen
			LOG.error("", e);
		}
		try {
			newVault.create(passwordField.getCharacters());
			LOG.info("Created new vault at path {}", vaultPath.get());
			String readmeFileName = resourceBundle.getString("addvault.new.readme.storageLocation.fileName");
			Path readmeFile = vaultPath.get().resolve(readmeFileName);
			try (WritableByteChannel ch = Files.newByteChannel(readmeFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
				ByteBuffer buf = StandardCharsets.US_ASCII.encode(readmeGenerator.createVaultStorageLocationReadmeRtf());
				ch.write(buf);
			}
			LOG.info("Created readme file {}", readmeFile);
			vault.set(newVault);
			vaults.add(newVault);
			window.setScene(successScene.get());
		} catch (IOException e) {
			// TODO show generic error screen
			LOG.error("", e);
		}
	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName.get();
	}

	public StringProperty vaultNameProperty() {
		return vaultName;
	}

	public IntegerProperty passwordStrengthProperty() {
		return passwordStrength;
	}

	public int getPasswordStrength() {
		return passwordStrength.get();
	}
}
