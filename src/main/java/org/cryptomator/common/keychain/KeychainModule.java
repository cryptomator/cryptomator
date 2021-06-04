package org.cryptomator.common.keychain;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;

import javax.inject.Singleton;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@Module
public class KeychainModule {

	@Provides
	@Singleton
	static Set<ServiceLoader.Provider<KeychainAccessProvider>> provideAvailableKeychainAccessProviderFactories() {
		return ServiceLoader.load(KeychainAccessProvider.class).stream().collect(Collectors.toUnmodifiableSet());
	}

	@Provides
	@Singleton
	static Set<KeychainAccessProvider> provideSupportedKeychainAccessProviders(Set<ServiceLoader.Provider<KeychainAccessProvider>> availableFactories) {
		return availableFactories.stream() //
				.map(ServiceLoader.Provider::get) //
				.filter(KeychainAccessProvider::isSupported) //
				.collect(Collectors.toUnmodifiableSet());
	}

	@Provides
	@Singleton
	static ObjectExpression<KeychainAccessProvider> provideKeychainAccessProvider(Settings settings, Set<KeychainAccessProvider> providers) {
		return Bindings.createObjectBinding(() -> {
			var selectedProviderClass = settings.keychainBackend().get().getProviderClass();
			var selectedProvider = providers.stream().filter(provider -> provider.getClass().getName().equals(selectedProviderClass)).findAny();
			var fallbackProvider = providers.stream().findAny().orElse(null);
			return selectedProvider.orElse(fallbackProvider);
		}, settings.keychainBackend());
	}

}
