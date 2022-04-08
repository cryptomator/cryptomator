package org.cryptomator.ui.keyloading;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingModule;
import org.cryptomator.ui.keyloading.masterkeyfile.MasterkeyFileLoadingModule;

import javax.inject.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

@Module(includes = {MasterkeyFileLoadingModule.class, HubKeyLoadingModule.class})
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
	static KeyLoadingStrategy provideKeyLoadingStrategy(@KeyLoading Vault vault, Map<String, Provider<KeyLoadingStrategy>> strategies) {
		try {
			String scheme = vault.getVaultConfigCache().get().getKeyId().getScheme();
			var fallback = KeyLoadingStrategy.failed(new IllegalArgumentException("Unsupported key id " + scheme));
			return strategies.getOrDefault(scheme, () -> fallback).get();
		} catch (IOException e) {
			return KeyLoadingStrategy.failed(e);
		}
	}

}
