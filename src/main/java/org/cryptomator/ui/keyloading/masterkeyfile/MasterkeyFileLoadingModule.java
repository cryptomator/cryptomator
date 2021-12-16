package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javafx.scene.Scene;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Module(subcomponents = {ForgetPasswordComponent.class, PassphraseEntryComponent.class})
public abstract class MasterkeyFileLoadingModule {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyFileLoadingModule.class);

	public enum MasterkeyFileProvision {
		MASTERKEYFILE_PROVIDED,
		CANCELED
	}

	@Provides
	@KeyLoadingScoped
	static UserInteractionLock<MasterkeyFileProvision> provideMasterkeyFileProvisionLock() {
		return new UserInteractionLock<>(null);
	}

	@Provides
	@Named("savedPassword")
	@KeyLoadingScoped
	static Optional<char[]> provideStoredPassword(KeychainManager keychain, @KeyLoading Vault vault) {
		if (!keychain.isSupported() || keychain.isLocked()) {
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
	@KeyLoadingScoped
	static AtomicReference<Path> provideUserProvidedMasterkeyPath() {
		return new AtomicReference<>();
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE)
	@KeyLoadingScoped
	static Scene provideUnlockSelectMasterkeyFileScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE);
	}

	@Binds
	@IntoMap
	@FxControllerKey(SelectMasterkeyFileController.class)
	abstract FxController bindUnlockSelectMasterkeyFileController(SelectMasterkeyFileController controller);

	@Binds
	@IntoMap
	@KeyLoadingScoped
	@StringKey(MasterkeyFileLoadingStrategy.SCHEME)
	abstract KeyLoadingStrategy bindMasterkeyFileLoadingStrategy(MasterkeyFileLoadingStrategy strategy);

}
