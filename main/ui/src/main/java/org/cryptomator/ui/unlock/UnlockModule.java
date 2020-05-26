package org.cryptomator.ui.unlock;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.keychain.KeychainManager;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Module(subcomponents = {ForgetPasswordComponent.class})
abstract class UnlockModule {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockModule.class);

	public enum PasswordEntry {PASSWORD_ENTERED, CANCELED}

	@Provides
	@UnlockScoped
	static UserInteractionLock<PasswordEntry> providePasswordEntryLock() {
		return new UserInteractionLock<>(null);
	}

	@Provides
	@Named("savedPassword")
	@UnlockScoped
	static Optional<char[]> provideStoredPassword(Optional<KeychainManager> keychain, @UnlockWindow Vault vault) {
		return keychain.map(k -> {
			try {
				return k.loadPassphrase(vault.getId());
			} catch (KeychainAccessException e) {
				LOG.error("Failed to load entry from system keychain.", e);
				return null;
			}
		});
	}

	@Provides
	@UnlockScoped
	static AtomicReference<char[]> providePassword(@Named("savedPassword") Optional<char[]> storedPassword) {
		return new AtomicReference(storedPassword.orElse(null));
	}

	@Provides
	@Named("savePassword")
	@UnlockScoped
	static AtomicBoolean provideSavePasswordFlag(@Named("savedPassword") Optional<char[]> storedPassword) {
		return new AtomicBoolean(storedPassword.isPresent());
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static Stage provideStage(StageFactory factory, @UnlockWindow Vault vault, @Named("unlockWindowOwner") Optional<Stage> owner) {
		Stage stage = factory.create();
		stage.setTitle(vault.getDisplayableName());
		stage.setResizable(false);
		if (owner.isPresent()) {
			stage.initOwner(owner.get());
			stage.initModality(Modality.WINDOW_MODAL);
		} else {
			stage.initModality(Modality.APPLICATION_MODAL);
		}
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK)
	@UnlockScoped
	static Scene provideUnlockScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SUCCESS)
	@UnlockScoped
	static Scene provideUnlockSuccessScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock_success.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT)
	@UnlockScoped
	static Scene provideInvalidMountPointScene(@UnlockWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/unlock_invalid_mount_point.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(UnlockController.class)
	abstract FxController bindUnlockController(UnlockController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UnlockSuccessController.class)
	abstract FxController bindUnlockSuccessController(UnlockSuccessController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UnlockInvalidMountPointController.class)
	abstract FxController bindUnlockInvalidMountPointController(UnlockInvalidMountPointController controller);

}
