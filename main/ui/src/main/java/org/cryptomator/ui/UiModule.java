/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.common.CommonsModule;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.jni.JniModule;
import org.cryptomator.keychain.KeychainModule;
import org.cryptomator.ui.controllers.ViewControllerModule;
import org.cryptomator.ui.model.VaultComponent;
import org.cryptomator.ui.util.DeferredCloser;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dagger.Module;
import dagger.Provides;
import javafx.beans.binding.Binding;

@Module(includes = {ViewControllerModule.class, CommonsModule.class, KeychainModule.class, JniModule.class}, subcomponents = {VaultComponent.class})
public class UiModule {

	private static final Logger LOG = LoggerFactory.getLogger(UiModule.class);

	@Provides
	@Singleton
	DeferredCloser provideDeferredCloser(@Named("shutdownTaskScheduler") Consumer<Runnable> shutdownTaskScheduler) {
		DeferredCloser closer = new DeferredCloser();
		shutdownTaskScheduler.accept(() -> {
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
	Binding<InetSocketAddress> provideServerSocketAddressBinding(Settings settings) {
		return EasyBind.combine(settings.useIpv6(), settings.port(), (useIpv6, port) -> {
			String host = useIpv6 ? "::1" : "localhost";
			return InetSocketAddress.createUnresolved(host, port.intValue());
		});
	}

	@Provides
	@Singleton
	WebDavServer provideWebDavServer(Binding<InetSocketAddress> serverSocketAddressBinding) {
		WebDavServer server = WebDavServer.create();
		// no need to unsubscribe eventually, because server is a singleton
		EasyBind.subscribe(serverSocketAddressBinding, server::bind);
		return server;
	}

}
