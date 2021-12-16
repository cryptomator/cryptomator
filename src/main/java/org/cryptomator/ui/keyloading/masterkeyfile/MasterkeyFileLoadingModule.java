package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Optional;

@Module(subcomponents = {ForgetPasswordComponent.class, PassphraseEntryComponent.class, ChooseMasterkeyFileComponent.class})
public abstract class MasterkeyFileLoadingModule {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyFileLoadingModule.class);

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

	@Binds
	@IntoMap
	@KeyLoadingScoped
	@StringKey(MasterkeyFileLoadingStrategy.SCHEME)
	abstract KeyLoadingStrategy bindMasterkeyFileLoadingStrategy(MasterkeyFileLoadingStrategy strategy);

}
