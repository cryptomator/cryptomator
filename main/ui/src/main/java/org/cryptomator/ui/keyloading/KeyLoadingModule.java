package org.cryptomator.ui.keyloading;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig.UnverifiedVaultConfig;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.keyloading.masterkeyfile.MasterkeyFileLoadingModule;

import javax.inject.Provider;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Module(includes = {MasterkeyFileLoadingModule.class})
abstract class KeyLoadingModule {

	@Provides
	@KeyLoading
	@KeyLoadingScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@KeyLoading
	@KeyLoadingScoped
	static Optional<URI> provideKeyId(@KeyLoading Vault vault) {
		return vault.getUnverifiedVaultConfig().map(UnverifiedVaultConfig::getKeyId);
	}

	@Provides
	@KeyLoading
	@KeyLoadingScoped
	static KeyLoadingStrategy provideKeyLoaderProvider(@KeyLoading Optional<URI> keyId, Map<String, KeyLoadingStrategy> strategies) {
		if (keyId.isEmpty()) {
			return KeyLoadingStrategy.failed(new IllegalArgumentException("No key id provided"));
		} else {
			String scheme = keyId.get().getScheme();
			var fallback = KeyLoadingStrategy.failed(new IllegalArgumentException("Unsupported key id " + scheme));
			return strategies.getOrDefault(scheme, fallback);
		}
	}

}
