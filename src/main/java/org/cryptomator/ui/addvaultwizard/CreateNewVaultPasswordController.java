package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.mount.MountService;
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
import java.util.List;
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
	private final List<MountService> mountProviders;
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
									 List<MountService> mountProviders, RecoveryKeyFactory recoveryKeyFactory, //
									 @Named("vaultName") StringProperty vaultName, //
									 ObjectProperty<Path> vaultPath, //
									 @AddVaultWizardWindow ObjectProperty<Vault> vault, //
									 @Named("recoveryKey") StringProperty recoveryKey, //
									 VaultListManager vaultListManager, //
									 ResourceBundle resourceBundle, //
									 @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
									 ReadmeGenerator readmeGenerator, //
									 SecureRandom csprng, //
									 MasterkeyFileAccess masterkeyFileAccess) {
		this.window = window;
		this.chooseExpertSettingsScene = chooseExpertSettingsScene;
		this.recoveryKeyScene = recoveryKeyScene;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.executor = executor;
		this.mountProviders = mountProviders;
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
		this.processing = new SimpleBooleanProperty();
		this.readyToCreateVault = new SimpleBooleanProperty();
		this.createVaultButtonState = Bindings.when(processing).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.shorteningThreshold = shorteningThreshold;
	}

	@FXML
	public void initialize() {
		readyToCreateVault.bind(newPasswordSceneController.goodPasswordProperty().and(recoveryKeyChoice.selectedToggleProperty().isNotNull()).and(processing.not()));
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
		}).onError(IOException.class, e -> {
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
		}).onError(IOException.class, e -> {
			LOG.error("Failed to create vault.", e);
			appWindows.showErrorWindow(e, window, window.getScene());
		}).andFinally(() -> {
			processing.set(false);
		}).runOnce(executor);
	}

	private void createVault(Path path) throws IOException {
		// 0. create directory
		Files.createDirectory(path);

		// 1. write masterkey:
		Path masterkeyFilePath = path.resolve(MASTERKEY_FILENAME);
		try (Masterkey masterkey = Masterkey.generate(csprng)) {
			masterkeyFileAccess.persist(masterkey, masterkeyFilePath, newPasswordSceneController.passwordField.getCharacters());

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

		LOG.info("Created vault at {}", path);
	}

	private void creationSucceeded(Path pathToVault) {
		try {
			Vault newVault = vaultListManager.add(pathToVault);
			postProcessVaultSettings(newVault.getVaultSettings());
			vaultProperty.set(newVault);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	//due to https://github.com/cryptomator/cryptomator/issues/2880#issuecomment-1680313498
	@Deprecated()
	private void postProcessVaultSettings(VaultSettings vaultSettings) {
		var nameOfWinfspLocalMounter = "org.cryptomator.frontend.fuse.mount.WinFspMountProvider";
		if (SystemUtils.IS_OS_WINDOWS //
				&& vaultSettings.path.get().toString().contains("Dropbox") //
				&& mountProviders.stream().anyMatch(s -> s.getClass().getName().equals(nameOfWinfspLocalMounter))) {
			vaultSettings.mountService.setValue(nameOfWinfspLocalMounter);
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
