/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui;

import static java.util.stream.Collectors.toList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.common.CommonsModule;
import org.cryptomator.crypto.engine.impl.CryptoEngineModule;
import org.cryptomator.cryptolib.CryptoLibModule;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.frontend.FrontendId;
import org.cryptomator.frontend.webdav.WebDavModule;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.jni.JniModule;
import org.cryptomator.keychain.KeychainModule;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultObjectMapperProvider;
import org.cryptomator.ui.model.Vaults;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.settings.SettingsProvider;
import org.cryptomator.ui.util.DeferredCloser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;
import dagger.Provides;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.stage.Stage;

@Module(includes = {CryptoEngineModule.class, CommonsModule.class, WebDavModule.class, KeychainModule.class, JniModule.class, CryptoLibModule.class})
class CryptomatorModule {

	private static final Logger LOG = LoggerFactory.getLogger(CryptomatorModule.class);
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
		Cryptomator.addShutdownTask(() -> {
			try {
				closer.close();
			} catch (Exception e) {
				LOG.error("Error during shutdown.", e);
			}
		});
		return closer;
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
	FrontendFactory provideFrontendFactory(DeferredCloser closer, WebDavServer webDavServer, Vaults vaults, Settings settings) {
		vaults.addListener((Observable o) -> setValidFrontendIds(webDavServer, vaults));
		setValidFrontendIds(webDavServer, vaults);
		webDavServer.setPort(settings.getPort());
		webDavServer.start();
		return closer.closeLater(webDavServer, WebDavServer::stop).get().orElseThrow(IllegalStateException::new);
	}

	private void setValidFrontendIds(WebDavServer webDavServer, Vaults vaults) {
		webDavServer.setValidFrontendIds(vaults.stream() //
				.map(Vault::getId).map(FrontendId::from).collect(toList()));
	}

}
