/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui;

import dagger.Module;
import dagger.Provides;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.keychain.KeychainModule;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// TODO move to common...
@Deprecated(forRemoval = true, since = "1.5.0")
@Module(includes = {KeychainModule.class})
public class UiModule {

	private static final int NUM_SCHEDULER_THREADS = 4;

	@Provides
	@Singleton
	ScheduledExecutorService provideScheduledExecutorService(@Named("shutdownTaskScheduler") Consumer<Runnable> shutdownTaskScheduler) {
		final AtomicInteger threadNumber = new AtomicInteger(1);
		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(NUM_SCHEDULER_THREADS, r -> {
			Thread t = new Thread(r);
			t.setName("Scheduler Thread " + threadNumber.getAndIncrement());
			t.setDaemon(true);
			return t;
		});
		shutdownTaskScheduler.accept(executorService::shutdown);
		return executorService;
	}

	// TODO @Binds abstract ExecutorService bindExecutorService(ScheduledExecutorService executor); ?
	@Provides
	@Singleton
	ExecutorService provideExecutorService(@Named("shutdownTaskScheduler") Consumer<Runnable> shutdownTaskScheduler) {
		final AtomicInteger threadNumber = new AtomicInteger(1);
		ExecutorService executorService = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r);
			t.setName("Background Thread " + threadNumber.getAndIncrement());
			t.setDaemon(true);
			return t;
		});
		shutdownTaskScheduler.accept(executorService::shutdown);
		return executorService;
	}

	@Provides
	@Singleton
	Binding<InetSocketAddress> provideServerSocketAddressBinding(Settings settings) {
		return Bindings.createObjectBinding(() -> {
			String host = SystemUtils.IS_OS_WINDOWS ? "127.0.0.1" : "localhost";
			return InetSocketAddress.createUnresolved(host, settings.port().intValue());
		}, settings.port());
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
