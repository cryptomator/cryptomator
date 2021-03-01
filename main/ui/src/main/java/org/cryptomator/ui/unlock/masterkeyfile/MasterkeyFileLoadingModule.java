package org.cryptomator.ui.unlock.masterkeyfile;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.cryptolib.common.MasterkeyFileLoader;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.scene.Scene;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Module(subcomponents = {ForgetPasswordComponent.class})
abstract class MasterkeyFileLoadingModule {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyFileLoadingModule.class);

	public enum PasswordEntry {
		PASSWORD_ENTERED,
		CANCELED
	}

	public enum MasterkeyFileProvision {
		MASTERKEYFILE_PROVIDED,
		CANCELED
	}

	@Provides
	@MasterkeyFileLoadingScoped
	static MasterkeyFileLoader provideMasterkeyFileLoader(MasterkeyFileAccess masterkeyFileAccess, @MasterkeyFileLoading Vault vault, MasterkeyFileLoadingContext context) {
		return masterkeyFileAccess.keyLoader(vault.getPath(), context);
	}

	@Provides
	@MasterkeyFileLoadingScoped
	static UserInteractionLock<PasswordEntry> providePasswordEntryLock() {
		return new UserInteractionLock<>(null);
	}

	@Provides
	@MasterkeyFileLoadingScoped
	static UserInteractionLock<MasterkeyFileProvision> provideMasterkeyFileProvisionLock() {
		return new UserInteractionLock<>(null);
	}

	@Provides
	@Named("savedPassword")
	@MasterkeyFileLoadingScoped
	static Optional<char[]> provideStoredPassword(KeychainManager keychain, @MasterkeyFileLoading Vault vault) {
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
	@MasterkeyFileLoadingScoped
	static AtomicReference<Path> provideUserProvidedMasterkeyPath() {
		return new AtomicReference<>();
	}

	@Provides
	@MasterkeyFileLoadingScoped
	static AtomicReference<char[]> providePassword(@Named("savedPassword") Optional<char[]> storedPassword) {
		return new AtomicReference<>(storedPassword.orElse(null));
	}

	@Provides
	@Named("savePassword")
	@MasterkeyFileLoadingScoped
	static AtomicBoolean provideSavePasswordFlag(@Named("savedPassword") Optional<char[]> storedPassword) {
		return new AtomicBoolean(storedPassword.isPresent());
	}

	@Provides
	@MasterkeyFileLoading
	@MasterkeyFileLoadingScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_ENTER_PASSWORD)
	@MasterkeyFileLoadingScoped
	static Scene provideUnlockScene(@MasterkeyFileLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_ENTER_PASSWORD);
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE)
	@MasterkeyFileLoadingScoped
	static Scene provideUnlockSelectMasterkeyFileScene(@MasterkeyFileLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE);
	}

	@Binds
	@IntoMap
	@FxControllerKey(PassphraseEntryController.class)
	abstract FxController bindUnlockController(PassphraseEntryController controller);

	@Binds
	@IntoMap
	@FxControllerKey(SelectMasterkeyFileController.class)
	abstract FxController bindUnlockSelectMasterkeyFileController(SelectMasterkeyFileController controller);


}
