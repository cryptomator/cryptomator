/*******************************************************************************
 * Copyright (c) 2014 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.DeferredCloser.Closer;
import org.cryptomator.webdav.WebDavServer;

import javafx.util.Callback;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class MainModule extends AbstractModule {
	DeferredCloser deferredCloser = new DeferredCloser();

	public static interface ControllerFactory extends Callback<Class<?>, Object> {

	}

	@Override
	protected void configure() {
		bind(DeferredCloser.class).toInstance(deferredCloser);
	}

	@Provides
	@Singleton
	ControllerFactory getControllerFactory(Injector injector) {
		return cls -> injector.getInstance(cls);
	}

	@Provides
	@Singleton
	ExecutorService getExec() {
		return closeLater(Executors.newCachedThreadPool(), ExecutorService::shutdown);
	}

	@Provides
	@Singleton
	Settings getSettings() {
		return closeLater(Settings.load(), Settings::save);
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
