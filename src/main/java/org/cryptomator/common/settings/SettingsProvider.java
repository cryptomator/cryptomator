/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Suppliers;
import org.cryptomator.common.Environment;
import org.cryptomator.integrations.quickaccess.QuickAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Singleton
public class SettingsProvider implements Supplier<Settings> {

	private static final ObjectMapper JSON = new ObjectMapper().setDefaultLeniency(true).registerModule(new JavaTimeModule());
	private static final Logger LOG = LoggerFactory.getLogger(SettingsProvider.class);
	private static final long SAVE_DELAY_MS = 1000;

	private final AtomicReference<ScheduledFuture<?>> scheduledSaveCmd = new AtomicReference<>();
	private final Supplier<Settings> settings = Suppliers.memoize(this::load);
	private final Environment env;
	private final ScheduledExecutorService scheduler;

	@Inject
	public SettingsProvider(Environment env, ScheduledExecutorService scheduler) {
		this.env = env;
		this.scheduler = scheduler;
	}

	@Override
	public Settings get() {
		return settings.get();
	}

	private Settings load() {
		Settings settings = env.getSettingsPath() //
				.flatMap(this::tryLoad) //
				.findFirst() //
				.orElseGet(() -> Settings.create(env));
		settings.setSaveCmd(this::scheduleSave);
		return settings;
	}

	private Stream<Settings> tryLoad(Path path) {
		LOG.debug("Attempting to load settings from {}", path);
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			var json = JSON.reader().readValue(in, SettingsJson.class);
			LOG.info("Settings loaded from {}", path);
			var settings = new Settings(json);
			return Stream.of(settings);
		} catch (JacksonException e) {
			LOG.warn("Failed to parse json file {}", path, e);
			return Stream.empty();
		} catch (NoSuchFileException e) {
			return Stream.empty();
		} catch (IOException e) {
			LOG.warn("Failed to load json file {}", path, e);
			return Stream.empty();
		}
	}

	private void scheduleSave(Settings settings) {
		if (settings == null) {
			return;
		}
		final Optional<Path> settingsPath = env.getSettingsPath().findFirst(); // always save to preferred (first) path
		settingsPath.ifPresent(path -> {
			Runnable saveCommand = () -> this.save(settings, path);
			ScheduledFuture<?> scheduledTask = scheduler.schedule(saveCommand, SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
			ScheduledFuture<?> previouslyScheduledTask = scheduledSaveCmd.getAndSet(scheduledTask);
			if (previouslyScheduledTask != null) {
				previouslyScheduledTask.cancel(false);
			}
		});
	}

	private void save(Settings settings, Path settingsPath) {
		assert settings != null : "method should only be invoked by #scheduleSave, which checks for null";
		LOG.debug("Attempting to save settings to {}", settingsPath);
		try {
			Files.createDirectories(settingsPath.getParent());
			Path tmpPath = settingsPath.resolveSibling(settingsPath.getFileName().toString() + ".tmp");
			try (OutputStream out = Files.newOutputStream(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				var jsonObj = settings.serialized();
				jsonObj.writtenByVersion = env.getAppVersion() + env.getBuildNumber().map("-"::concat).orElse("");
				JSON.writerWithDefaultPrettyPrinter().writeValue(out, jsonObj);
			}
			Files.move(tmpPath, settingsPath, StandardCopyOption.REPLACE_EXISTING);
			LOG.info("Settings saved to {}", settingsPath);
		} catch (IOException e) {
			LOG.error("Failed to save settings.", e);
		}
	}

}
