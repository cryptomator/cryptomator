package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.forgetpassword.ForgetPasswordComponent;
import org.cryptomator.ui.keyloading.IdentitySelectionComponent;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Optional;

@Module(subcomponents = {ForgetPasswordComponent.class, PassphraseEntryComponent.class, ChooseMasterkeyFileComponent.class, IdentitySelectionComponent.class})
public interface MasterkeyFileLoadingModule {

	@Provides
	@Named("savedPassword")
	@KeyLoadingScoped
	static Optional<char[]> provideStoredPassword(KeychainManager keychain, @KeyLoading Vault vault) {
		if (!keychain.isSupported() || keychain.isLocked()) {
			return Optional.empty();
		}
		
		// Don't use saved passwords for multi-identity vaults
		// Each identity may have a different password
		try {
			var manager = vault.getIdentityProvider().getManager();
			if (manager.getIdentities().size() > 1) {
				// Multiple identities - user must enter password each time
				return Optional.empty();
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(MasterkeyFileLoadingModule.class).warn("Failed to check identity count, skipping saved password", e);
			return Optional.empty();
		}
		
		// Single identity - can use saved password
		try {
			return Optional.ofNullable(keychain.loadPassphrase(vault.getId()));
		} catch (KeychainAccessException e) {
			LoggerFactory.getLogger(MasterkeyFileLoadingModule.class).error("Failed to load entry from system keychain.", e);
			return Optional.empty();
		}
	}

	@Binds
	@IntoMap
	@KeyLoadingScoped
	@StringKey(MasterkeyFileLoadingStrategy.SCHEME)
	abstract KeyLoadingStrategy bindMasterkeyFileLoadingStrategy(MasterkeyFileLoadingStrategy strategy);

}
