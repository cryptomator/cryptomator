package org.cryptomator.common.keychain;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;

import javax.inject.Singleton;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import java.util.List;

@Module
public class KeychainModule {

	@Provides
	@Singleton
	static List<KeychainAccessProvider> provideSupportedKeychainAccessProviders() {
		return KeychainAccessProvider.get().toList();
	}

	@Provides
	@Singleton
	static ObjectExpression<KeychainAccessProvider> provideKeychainAccessProvider(Settings settings, List<KeychainAccessProvider> providers) {
		return Bindings.createObjectBinding(() -> {
			if (!settings.useKeychain.get()) {
				return null;
			}
			var selectedProviderClass = settings.keychainProvider.get();
			var selectedProvider = providers.stream().filter(provider -> provider.getClass().getName().equals(selectedProviderClass)).findAny();
			var fallbackProvider = providers.stream().findFirst().orElse(null);
			return selectedProvider.orElse(fallbackProvider);
		}, settings.keychainProvider, settings.useKeychain);
	}

}
