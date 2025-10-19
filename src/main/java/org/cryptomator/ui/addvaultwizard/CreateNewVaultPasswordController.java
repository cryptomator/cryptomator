package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.recoverykey.RecoveryKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.cryptomator.common.Constants.DEFAULT_KEY_ID;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@AddVaultWizardScoped
public class CreateNewVaultPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CreateNewVaultPasswordController.class);

	private final Stage window;
	private final Lazy<Scene> chooseExpertSettingsScene;
	private final Lazy<Scene> recoveryKeyScene;
	private final Lazy<Scene> successScene;
	private final FxApplicationWindows appWindows;
	private final ExecutorService executor;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final StringProperty vaultNameProperty;
	private final ObjectProperty<Path> vaultPathProperty;
	private final ObjectProperty<Vault> vaultProperty;
	private final StringProperty recoveryKeyProperty;
	private final VaultListManager vaultListManager;
	private final ResourceBundle resourceBundle;
	private final ReadmeGenerator readmeGenerator;
	private final SecureRandom csprng;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final MultiKeyslotFile multiKeyslotFile;
	private final BooleanProperty processing;
	private final BooleanProperty readyToCreateVault;
	private final ObjectBinding<ContentDisplay> createVaultButtonState;
	private final IntegerProperty shorteningThreshold;

	/* FXML */
	public ToggleGroup recoveryKeyChoice;
	public Toggle showRecoveryKey;
	public Toggle skipRecoveryKey;
	public NewPasswordController newPasswordSceneController;

	@Inject
	CreateNewVaultPasswordController(@AddVaultWizardWindow Stage window, //
									 @FxmlScene(FxmlFile.ADDVAULT_NEW_EXPERT_SETTINGS) Lazy<Scene> chooseExpertSettingsScene, //
									 @FxmlScene(FxmlFile.ADDVAULT_NEW_RECOVERYKEY) Lazy<Scene> recoveryKeyScene, //
									 @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, //
									 FxApplicationWindows appWindows, //
									 ExecutorService executor, //
									 RecoveryKeyFactory recoveryKeyFactory, //
									 @Named("vaultName") StringProperty vaultName, //
									 ObjectProperty<Path> vaultPath, //
									 @AddVaultWizardWindow ObjectProperty<Vault> vault, //
									 @Named("recoveryKey") StringProperty recoveryKey, //
									 VaultListManager vaultListManager, //
									 ResourceBundle resourceBundle, //
								 @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
								 ReadmeGenerator readmeGenerator, //
								 SecureRandom csprng, //
								 MasterkeyFileAccess masterkeyFileAccess, //
								 MultiKeyslotFile multiKeyslotFile) {
		this.window = window;
		this.chooseExpertSettingsScene = chooseExpertSettingsScene;
		this.recoveryKeyScene = recoveryKeyScene;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.executor = executor;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.vaultNameProperty = vaultName;
		this.vaultPathProperty = vaultPath;
		this.vaultProperty = vault;
		this.recoveryKeyProperty = recoveryKey;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;
		this.readmeGenerator = readmeGenerator;
		this.csprng = csprng;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.multiKeyslotFile = multiKeyslotFile;
		this.processing = new SimpleBooleanProperty();
		this.readyToCreateVault = new SimpleBooleanProperty();
		this.createVaultButtonState = Bindings.when(processing).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.shorteningThreshold = shorteningThreshold;
	}

	@FXML
	public void initialize() {
		LOG.info("Initializing CreateNewVaultPasswordController");
		
		if (newPasswordSceneController == null) {
			LOG.error("newPasswordSceneController is null!");
			return;
		}
		if (recoveryKeyChoice == null) {
			LOG.error("recoveryKeyChoice is null!");
			return;
		}
		
		LOG.info("Binding readyToCreateVault property");
		readyToCreateVault.bind(newPasswordSceneController.goodPasswordProperty().and(recoveryKeyChoice.selectedToggleProperty().isNotNull()).and(processing.not()));
		
		// Debug bindings
		newPasswordSceneController.goodPasswordProperty().addListener((obs, oldVal, newVal) -> {
			LOG.info("Good password changed: {}", newVal);
		});
		recoveryKeyChoice.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			LOG.info("Recovery key choice changed: {}", newVal);
		});
		processing.addListener((obs, oldVal, newVal) -> {
			LOG.info("Processing changed: {}", newVal);
		});
		readyToCreateVault.addListener((obs, oldVal, newVal) -> {
			LOG.info("Ready to create vault: {}", newVal);
		});
		
		window.setOnHiding(event -> {
			newPasswordSceneController.passwordField.wipe();
			newPasswordSceneController.reenterField.wipe();
		});
	}

	@FXML
	public void back() {
		window.setScene(chooseExpertSettingsScene.get());
	}

	@FXML
	public void next() {
		LOG.info("Create vault button clicked");
		Path vaultPath = vaultPathProperty.get();
		if (vaultPath == null) {
			LOG.error("Vault path is null!");
			appWindows.showErrorWindow(new IllegalStateException("Vault path is not set"), window, window.getScene());
			return;
		}
		LOG.info("Vault will be created at: {}", vaultPath);
		
		if (showRecoveryKey.equals(recoveryKeyChoice.getSelectedToggle())) {
			showRecoveryKeyScene();
		} else if (skipRecoveryKey.equals(recoveryKeyChoice.getSelectedToggle())) {
			showSuccessScene();
		} else {
			throw new IllegalStateException("Unexpected toggle state");
		}
	}

	private void showRecoveryKeyScene() {
		Path pathToVault = vaultPathProperty.get();
		processing.set(true);
		Tasks.create(() -> {
			createVault(pathToVault);
			return recoveryKeyFactory.createRecoveryKey(pathToVault, newPasswordSceneController.passwordField.getCharacters());
		}).onSuccess(recoveryKey -> {
			creationSucceeded(pathToVault);
			recoveryKeyProperty.set(recoveryKey);
			window.setScene(recoveryKeyScene.get());
		}).onError(Exception.class, e -> {
			LOG.error("Failed to create vault.", e);
			appWindows.showErrorWindow(e, window, window.getScene());
		}).andFinally(() -> {
			processing.set(false);
		}).runOnce(executor);
	}

	private void showSuccessScene() {
		Path pathToVault = vaultPathProperty.get();
		processing.set(true);
		Tasks.create(() -> {
			createVault(pathToVault);
		}).onSuccess(() -> {
			creationSucceeded(pathToVault);
			window.setScene(successScene.get());
		}).onError(Exception.class, e -> {
			LOG.error("Failed to create vault.", e);
			appWindows.showErrorWindow(e, window, window.getScene());
		}).andFinally(() -> {
			processing.set(false);
		}).runOnce(executor);
	}

	private void createVault(Path path) throws IOException {
		// 0. create directory
		if (Files.exists(path)) {
			if (!Files.isDirectory(path)) {
				throw new IOException("Path exists but is not a directory: " + path);
			}
			// Check if directory is empty
			try (var stream = Files.list(path)) {
				if (stream.findAny().isPresent()) {
					throw new IOException("Directory already exists and is not empty: " + path);
				}
			}
			LOG.info("Using existing empty directory at {}", path);
		} else {
			Files.createDirectory(path);
		}

		// 1. write masterkey and initialize identity:
		try (Masterkey masterkey = Masterkey.generate(csprng)) {
			// Note: IdentityInitializer handles masterkey persistence now
			
		// Initialize primary identity (this will create the masterkey file)
		org.cryptomator.common.recovery.IdentityInitializer.initializePrimaryIdentity(
			path, 
			"Primary", 
			"Default vault identity", 
			masterkey, 
			newPasswordSceneController.passwordField.getCharacters(),
			multiKeyslotFile
		);

			// 2. initialize vault:
			try {
				MasterkeyLoader loader = ignored -> masterkey.copy();
				CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
						.withCipherCombo(CryptorProvider.Scheme.SIV_GCM) //
						.withKeyLoader(loader) //
						.withShorteningThreshold(shorteningThreshold.get()) //
						.build();
				CryptoFileSystemProvider.initialize(path, fsProps, DEFAULT_KEY_ID);

				// 3. write vault-internal readme file:
				String vaultReadmeFileName = resourceBundle.getString("addvault.new.readme.accessLocation.fileName");
				try (FileSystem fs = CryptoFileSystemProvider.newFileSystem(path, fsProps); //
					 WritableByteChannel ch = Files.newByteChannel(fs.getPath("/", vaultReadmeFileName), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
					ch.write(US_ASCII.encode(readmeGenerator.createVaultAccessLocationReadmeRtf()));
				}
			} catch (CryptoException e) {
				throw new IOException("Vault initialization failed", e);
			}
		}

		// 4. write vault-external readme file:
		String storagePathReadmeFileName = resourceBundle.getString("addvault.new.readme.storageLocation.fileName");
		try (WritableByteChannel ch = Files.newByteChannel(path.resolve(storagePathReadmeFileName), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			ch.write(US_ASCII.encode(readmeGenerator.createVaultStorageLocationReadmeRtf()));
		}

		LOG.info("Created vault at {} with primary identity", path);
	}

	private void creationSucceeded(Path pathToVault) {
		try {
			Vault newVault = vaultListManager.add(pathToVault);
			vaultProperty.set(newVault);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultNameProperty.get();
	}

	public StringProperty vaultNameProperty() {
		return vaultNameProperty;
	}

	public BooleanProperty readyToCreateVaultProperty() {
		return readyToCreateVault;
	}

	public boolean isReadyToCreateVault() {
		return readyToCreateVault.get();
	}

	public ObjectBinding<ContentDisplay> createVaultButtonStateProperty() {
		return createVaultButtonState;
	}

	public ContentDisplay getCreateVaultButtonState() {
		return createVaultButtonState.get();
	}
}
