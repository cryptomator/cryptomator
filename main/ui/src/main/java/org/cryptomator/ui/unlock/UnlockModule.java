package org.cryptomator.ui.unlock;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
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
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Module(subcomponents = {ForgetPasswordComponent.class})
abstract class UnlockModule {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockModule.class);

	public enum PasswordEntry {
		PASSWORD_ENTERED,
		CANCELED
	}

	@Provides
	@UnlockScoped
	static UserInteractionLock<PasswordEntry> providePasswordEntryLock() {
		return new UserInteractionLock<>(null);
	}

	@Provides
	@Named("savedPassword")
	@UnlockScoped
	static Optional<char[]> provideStoredPassword(KeychainManager keychain, @UnlockWindow Vault vault) {
		if (!keychain.isSupported()) {
			return Optional.empty();
		} else {
			try {
				return Optional.ofNullable(keychain.loadPassphrase(vault.getId()));
			} catch (KeychainAccessException e) {
				LOG.error("Failed to load entry from system keychain.", e);
				return Optional.empty();
			}
		}
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
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static Stage provideStage(StageFactory factory, @UnlockWindow Vault vault, @Named("unlockWindowOwner") Optional<Stage> owner) {
		Stage stage = factory.create();
		stage.setTitle(vault.getDisplayName());
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
	static Scene provideUnlockScene(@UnlockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK);
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SUCCESS)
	@UnlockScoped
	static Scene provideUnlockSuccessScene(@UnlockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_SUCCESS);
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT)
	@UnlockScoped
	static Scene provideInvalidMountPointScene(@UnlockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT);
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
