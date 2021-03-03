package org.cryptomator.ui.unlock;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig.UnverifiedVaultConfig;
import org.cryptomator.ui.unlock.masterkeyfile.MasterkeyFileLoadingComponent;

import javafx.stage.Stage;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Module(subcomponents = {MasterkeyFileLoadingComponent.class})
abstract class KeyLoadingModule {

	@Provides
	@UnlockScoped
	static Optional<URI> provideKeyId(@UnlockWindow Vault vault) {
		return vault.getUnverifiedVaultConfig().map(UnverifiedVaultConfig::getKeyId);
	}

	@Provides
	@UnlockScoped
	static KeyLoadingComponent provideKeyLoaderProvider(Optional<URI> keyId, Map<String, KeyLoadingComponent> keyLoaderProviders) {
		if (keyId.isEmpty()) {
			return KeyLoadingComponent.exceptional(new IllegalArgumentException("No key id provided"));
		} else {
			String scheme = keyId.get().getScheme();
			return keyLoaderProviders.getOrDefault(scheme, KeyLoadingComponent.exceptional(new IllegalArgumentException("Unsupported key id " + scheme)));
		}
	}

	@Provides
	@IntoMap
	@StringKey("masterkeyfile")
	static KeyLoadingComponent provideMasterkeyFileLoadingComponet(MasterkeyFileLoadingComponent.Builder compBuilder, @UnlockWindow Stage window, @UnlockWindow Vault vault) {
		return compBuilder.unlockWindow(window).vault(vault).build();
	}

}
