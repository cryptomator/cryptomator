package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.forgetpassword.ForgetPasswordComponent;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Objects;
import java.util.Optional;

@Module(subcomponents = {ForgetPasswordComponent.class, PassphraseEntryComponent.class, ChooseMasterkeyFileComponent.class})
public interface MasterkeyFileLoadingModule {

	/**
	 * Key loading may run for a yet-to-setup vault (i.e. deeplink with only a config) - Masterkey loading requires in the current implementation
	 * an already setup vault.
	 */
	String NO_LOCAL_VAULT = "masterkey file loading requires a local vault";

	@Provides
	@Named("savedPassword")
	@KeyLoadingScoped
	static Optional<char[]> provideStoredPassword(KeychainManager keychain, @KeyLoading @Nullable Vault vault) {
		if (!keychain.isSupported() || keychain.isLocked()) {
			return Optional.empty();
		} else {
			try {
				return Optional.ofNullable(keychain.loadPassphrase(Objects.requireNonNull(vault, NO_LOCAL_VAULT).getId()));
			} catch (KeychainAccessException e) {
				LoggerFactory.getLogger(MasterkeyFileLoadingModule.class).error("Failed to load entry from system keychain.", e);
				return Optional.empty();
			}
		}
	}

	@Binds
	@IntoMap
	@KeyLoadingScoped
	@StringKey(MasterkeyFileLoadingStrategy.SCHEME)
	abstract KeyLoadingStrategy bindMasterkeyFileLoadingStrategy(MasterkeyFileLoadingStrategy strategy);

}
