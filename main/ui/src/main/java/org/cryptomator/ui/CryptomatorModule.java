package org.cryptomator.ui;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.filesystem.crypto.CryptoFileSystemModule;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.mount.WebDavMounter;
import org.cryptomator.frontend.webdav.mount.WebDavMounterProvider;
import org.cryptomator.ui.model.VaultObjectMapperProvider;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.settings.SettingsProvider;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.DeferredCloser.Closer;
import org.cryptomator.ui.util.SemVerComparator;

import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;
import javafx.application.Application;

@Module(includes = CryptoFileSystemModule.class)
class CryptomatorModule {

	private final Application application;
	private final DeferredCloser deferredCloser;

	public CryptomatorModule(Application application) {
		this.application = application;
		this.deferredCloser = new DeferredCloser();
	}

	@Provides
	@Singleton
	Application provideApplication() {
		return application;
	}

	@Provides
	@Singleton
	DeferredCloser provideDeferredCloser() {
		return deferredCloser;
	}

	@Provides
	@Singleton
	@Named("SemVer")
	Comparator<String> provideSemVerComparator() {
		return new SemVerComparator();
	}

	@Provides
	@Singleton
	@Named("VaultJsonMapper")
	ObjectMapper provideVaultObjectMapper(VaultObjectMapperProvider vaultObjectMapperProvider) {
		return vaultObjectMapperProvider.get();
	}

	@Provides
	@Singleton
	Settings provideSettings(SettingsProvider settingsProvider) {
		return settingsProvider.get();
	}

	@Provides
	@Singleton
	ExecutorService provideExecutorService() {
		return closeLater(Executors.newCachedThreadPool(), ExecutorService::shutdown);
	}

	@Provides
	@Singleton
	WebDavMounter provideWebDavMounter(WebDavMounterProvider webDavMounterProvider) {
		return webDavMounterProvider.get();
	}

	@Provides
	@Singleton
	FrontendFactory provideFrontendFactory(WebDavServer webDavServer) {
		webDavServer.start();
		return closeLater(webDavServer, WebDavServer::stop);
	}

	private <T> T closeLater(T object, Closer<T> closer) {
		return deferredCloser.closeLater(object, closer).get().get();
	}

}
