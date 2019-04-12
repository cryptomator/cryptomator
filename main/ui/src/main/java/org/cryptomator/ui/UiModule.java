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
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.keychain.KeychainModule;
import org.cryptomator.ui.controllers.ViewControllerModule;
import org.cryptomator.ui.model.VaultComponent;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Named;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Module(includes = {ViewControllerModule.class, KeychainModule.class}, subcomponents = {VaultComponent.class})
public class UiModule {

	private static final int NUM_SCHEDULER_THREADS = 4;

	@Provides
	@FxApplicationScoped
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

	@Provides
	@FxApplicationScoped
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
	@FxApplicationScoped
	Binding<InetSocketAddress> provideServerSocketAddressBinding(Settings settings) {
		return EasyBind.map(settings.port(), (Number port) -> {
			String host = SystemUtils.IS_OS_WINDOWS ? "127.0.0.1" : "localhost";
			return InetSocketAddress.createUnresolved(host, port.intValue());
		});
	}

	@Provides
	@FxApplicationScoped
	WebDavServer provideWebDavServer(Binding<InetSocketAddress> serverSocketAddressBinding) {
		WebDavServer server = WebDavServer.create();
		// no need to unsubscribe eventually, because server is a singleton
		EasyBind.subscribe(serverSocketAddressBinding, server::bind);
		return server;
	}

}
