/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.cryptomator.common.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Singleton
public class SettingsProvider implements Supplier<Settings> {

	private static final Logger LOG = LoggerFactory.getLogger(SettingsProvider.class);
	private static final long SAVE_DELAY_MS = 1000;

	private final AtomicReference<ScheduledFuture<?>> scheduledSaveCmd = new AtomicReference<>();
	private final Supplier<Settings> settings = Suppliers.memoize(this::load);
	private final SettingsJsonAdapter settingsJsonAdapter;
	private final Environment env;
	private final ScheduledExecutorService scheduler;
	private final Gson gson;

	@Inject
	public SettingsProvider(SettingsJsonAdapter settingsJsonAdapter, Environment env, ScheduledExecutorService scheduler) {
		this.settingsJsonAdapter = settingsJsonAdapter;
		this.env = env;
		this.scheduler = scheduler;
		this.gson = new GsonBuilder() //
				.setPrettyPrinting().setLenient().disableHtmlEscaping() //
				.registerTypeAdapter(Settings.class, settingsJsonAdapter) //
				.create();
	}

	@Override
	public Settings get() {
		return settings.get();
	}

	private Settings load() {
		Settings settings = env.getSettingsPath().flatMap(this::tryLoad).findFirst().orElse(new Settings(env));
		settings.setSaveCmd(this::scheduleSave);
		return settings;
	}

	private Stream<Settings> tryLoad(Path path) {
		LOG.debug("Attempting to load settings from {}", path);
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ); //
			 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			JsonElement json = JsonParser.parseReader(reader);
			if (json.isJsonObject()) {
				Settings settings = gson.fromJson(json, Settings.class);
				LOG.info("Settings loaded from {}", path);
				return Stream.of(settings);
			} else {
				LOG.warn("Invalid json file {}", path);
				return Stream.empty();
			}
		} catch (NoSuchFileException e) {
			return Stream.empty();
		} catch (IOException | JsonParseException e) {
			LOG.warn("Exception while loading settings from " + path, e);
			return Stream.empty();
		}
	}

	private void scheduleSave(Settings settings) {
		if (settings == null) {
			return;
		}
		final Optional<Path> settingsPath = env.getSettingsPath().findFirst(); // alway save to preferred (first) path
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
			try (OutputStream out = Files.newOutputStream(tmpPath, StandardOpenOption.CREATE_NEW); //
				 Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
				gson.toJson(settings, writer);
			}
			Files.move(tmpPath, settingsPath, StandardCopyOption.REPLACE_EXISTING);
			LOG.info("Settings saved to {}", settingsPath);
		} catch (IOException | JsonParseException e) {
			LOG.error("Failed to save settings.", e);
		}
	}

}
