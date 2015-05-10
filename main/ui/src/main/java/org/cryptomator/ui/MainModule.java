/*******************************************************************************
 * Copyright (c) 2014 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.util.Callback;

import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.SamplingCryptorDecorator;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.MainApplication.MainApplicationReference;
import org.cryptomator.ui.model.VaultFactory;
import org.cryptomator.ui.model.VaultObjectMapperProvider;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.settings.SettingsProvider;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.DeferredCloser.Closer;
import org.cryptomator.ui.util.SemVerComparator;
import org.cryptomator.ui.util.mount.WebDavMounter;
import org.cryptomator.ui.util.mount.WebDavMounterProvider;
import org.cryptomator.webdav.WebDavServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Names;

public class MainModule extends AbstractModule {

	private final DeferredCloser deferredCloser = new DeferredCloser();

	public static interface ControllerFactory extends Callback<Class<?>, Object> {

	}

	@Override
	protected void configure() {
		bind(DeferredCloser.class).toInstance(deferredCloser);
		bind(ObjectMapper.class).annotatedWith(Names.named("VaultJsonMapper")).toProvider(VaultObjectMapperProvider.class);
		bind(Settings.class).toProvider(SettingsProvider.class);
		bind(WebDavMounter.class).toProvider(WebDavMounterProvider.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	ControllerFactory getControllerFactory(Injector injector) {
		return cls -> injector.getInstance(cls);
	}

	@Provides
	@Singleton
	MainApplicationReference getApplicationBinding() {
		return new MainApplicationReference();
	}

	@Provides
	Application getApplication(MainApplicationReference ref) {
		return ref.get();
	}

	@Provides
	@Named("SemVer")
	@Singleton
	Comparator<String> getSemVerComparator() {
		return new SemVerComparator();
	}

	@Provides
	@Singleton
	ExecutorService getExec() {
		return closeLater(Executors.newCachedThreadPool(), ExecutorService::shutdown);
	}

	@Provides
	Cryptor getCryptor() {
		return SamplingCryptorDecorator.decorate(new Aes256Cryptor());
	}

	@Provides
	@Singleton
	VaultFactory getVaultFactory(WebDavServer server, Provider<Cryptor> cryptorProvider, WebDavMounter mounter, DeferredCloser closer) {
		return new VaultFactory(server, cryptorProvider, mounter, closer);
	}

	@Provides
	@Singleton
	WebDavServer getServer() {
		final WebDavServer webDavServer = new WebDavServer();
		webDavServer.start();
		return closeLater(webDavServer, WebDavServer::stop);
	}

	<T> T closeLater(T object, Closer<T> closer) {
		return deferredCloser.closeLater(object, closer).get().get();
	}
}
