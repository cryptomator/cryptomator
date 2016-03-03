/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.impl.CryptoEngineModule;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.mount.WebDavMounter;
import org.cryptomator.frontend.webdav.mount.WebDavMounterProvider;
import org.cryptomator.ui.model.VaultObjectMapperProvider;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.settings.SettingsProvider;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.SemVerComparator;

import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;
import javafx.application.Application;
import javafx.stage.Stage;

@Module(includes = CryptoEngineModule.class)
class CryptomatorModule {

	private final Application application;
	private final Stage mainWindow;

	public CryptomatorModule(Application application, Stage mainWindow) {
		this.application = application;
		this.mainWindow = mainWindow;
	}

	@Provides
	@Singleton
	Application provideApplication() {
		return application;
	}

	@Provides
	@Singleton
	@Named("mainWindow")
	Stage provideMainWindow() {
		return mainWindow;
	}

	@Provides
	@Singleton
	DeferredCloser provideDeferredCloser() {
		DeferredCloser closer = new DeferredCloser();
		Cryptomator.addShutdownTask(closer::close);
		return closer;
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
	ExecutorService provideExecutorService(DeferredCloser closer) {
		return closer.closeLater(Executors.newCachedThreadPool(), ExecutorService::shutdown).get().orElseThrow(IllegalStateException::new);
	}

	@Provides
	@Singleton
	WebDavMounter provideWebDavMounter(WebDavMounterProvider webDavMounterProvider) {
		return webDavMounterProvider.get();
	}

	@Provides
	@Singleton
	FrontendFactory provideFrontendFactory(DeferredCloser closer, WebDavServer webDavServer, Settings settings) {
		webDavServer.setPort(settings.getPort());
		webDavServer.start();
		return closer.closeLater(webDavServer, WebDavServer::stop).get().orElseThrow(IllegalStateException::new);
	}

}
