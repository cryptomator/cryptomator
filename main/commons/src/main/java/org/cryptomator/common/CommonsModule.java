/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import com.tobiasdiez.easybind.EasyBind;
import dagger.Module;
import dagger.Provides;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultComponent;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Module(subcomponents = {VaultComponent.class})
public abstract class CommonsModule {

	private static final Logger LOG = LoggerFactory.getLogger(CommonsModule.class);
	private static final int NUM_SCHEDULER_THREADS = 2;
	private static final int NUM_CORE_BG_THREADS = 6;
	private static final long BG_THREAD_KEEPALIVE_SECONDS = 60l;

	@Provides
	@Singleton
	@Named("licensePublicKey")
	static String provideLicensePublicKey() {
		// in PEM format without the dash-escaped begin/end lines
		return "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQB7NfnqiZbg2KTmoflmZ71PbXru7oW" //
				+ "fmnV2yv3eDjlDfGruBrqz9TtXBZV/eYWt31xu1osIqaT12lKBvZ511aaAkIBeOEV" //
				+ "gwcBIlJr6kUw7NKzeJt7r2rrsOyQoOG2nWc/Of/NBqA3mIZRHk5Aq1YupFdD26QE" //
				+ "r0DzRyj4ixPIt38CQB8=";
	}

	@Provides
	@Singleton
	@Named("SemVer")
	static Comparator<String> providesSemVerComparator() {
		return new SemVerComparator();
	}

	@Provides
	@Singleton
	static Settings provideSettings(SettingsProvider settingsProvider) {
		return settingsProvider.get();
	}

	@Provides
	@Singleton
	static ObservableList<Vault> provideVaultList(VaultListManager vaultListManager) {
		return vaultListManager.getVaultList();
	}

	@Provides
	@Singleton
	static ScheduledExecutorService provideScheduledExecutorService(ShutdownHook shutdownHook) {
		final AtomicInteger threadNumber = new AtomicInteger(1);
		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(NUM_SCHEDULER_THREADS, r -> {
			String name = String.format("App Scheduled Executor %02d", threadNumber.getAndIncrement());
			Thread t = new Thread(r);
			t.setName(name);
			t.setUncaughtExceptionHandler(CommonsModule::handleUncaughtExceptionInBackgroundThread);
			t.setDaemon(true);
			LOG.debug("Starting {}", t.getName());
			return t;
		});
		shutdownHook.runOnShutdown(executorService::shutdown);
		return executorService;
	}

	@Provides
	@Singleton
	static ExecutorService provideExecutorService(ShutdownHook shutdownHook) {
		final AtomicInteger threadNumber = new AtomicInteger(1);
		ExecutorService executorService = new ThreadPoolExecutor(NUM_CORE_BG_THREADS, Integer.MAX_VALUE, BG_THREAD_KEEPALIVE_SECONDS, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
			String name = String.format("App Background Thread %03d", threadNumber.getAndIncrement());
			Thread t = new Thread(r);
			t.setName(name);
			t.setUncaughtExceptionHandler(CommonsModule::handleUncaughtExceptionInBackgroundThread);
			t.setDaemon(true);
			LOG.debug("Starting {}", t.getName());
			return t;
		});
		shutdownHook.runOnShutdown(executorService::shutdown);
		return executorService;
	}

	private static void handleUncaughtExceptionInBackgroundThread(Thread thread, Throwable throwable) {
		LOG.error("Uncaught exception in " + thread.getName(), throwable);
	}

	@Provides
	@Singleton
	static Binding<InetSocketAddress> provideServerSocketAddressBinding(Settings settings) {
		return Bindings.createObjectBinding(() -> {
			String host = SystemUtils.IS_OS_WINDOWS ? "127.0.0.1" : "localhost";
			return InetSocketAddress.createUnresolved(host, settings.port().intValue());
		}, settings.port());
	}

	@Provides
	@Singleton
	static WebDavServer provideWebDavServer(Binding<InetSocketAddress> serverSocketAddressBinding) {
		WebDavServer server = WebDavServer.create();
		// no need to unsubscribe eventually, because server is a singleton
		EasyBind.subscribe(serverSocketAddressBinding, server::bind);
		return server;
	}

}
