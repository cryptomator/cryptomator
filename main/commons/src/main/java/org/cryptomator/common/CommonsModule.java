/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import dagger.Binds;
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
import org.fxmisc.easybind.EasyBind;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Module(subcomponents = {VaultComponent.class})
public abstract class CommonsModule {

	private static final int NUM_SCHEDULER_THREADS = 4;

	@Provides
	@Singleton
	@Named("licensePublicKey")
	static String provideLicensePublicKey() {
		// TODO replace
		return "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBgc4HZz+/fBbC7lmEww0AO3NK9wVZ" //
				+ "PDZ0VEnsaUFLEYpTzb90nITtJUcPUbvOsdZIZ1Q8fnbquAYgxXL5UgHMoywAib47" //
				+ "6MkyyYgPk0BXZq3mq4zImTRNuaU9slj9TVJ3ScT3L1bXwVuPJDzpr5GOFpaj+WwM" //
				+ "Al8G7CqwoJOsW7Kddns=";
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
			Thread t = new Thread(r);
			t.setName("Background Thread " + threadNumber.getAndIncrement());
			t.setDaemon(true);
			return t;
		});
		shutdownHook.runOnShutdown(executorService::shutdown);
		return executorService;
	}

	@Binds
	@Singleton
	abstract ExecutorService bindExecutorService(ScheduledExecutorService executor);

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
