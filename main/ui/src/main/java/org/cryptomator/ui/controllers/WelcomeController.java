/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.util.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.cryptomator.ui.util.DialogBuilderUtil.buildYesNoDialog;

@FxApplicationScoped
public class WelcomeController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);

	private final Application app;
	private final Optional<String> applicationVersion;
	private final Localization localization;
	private final Settings settings;
	private final Comparator<String> semVerComparator;
	private final ScheduledExecutorService executor;

	@Inject
	public WelcomeController(Application app, @Named("applicationVersion") Optional<String> applicationVersion, Localization localization, Settings settings, @Named("SemVer") Comparator<String> semVerComparator,
							 ScheduledExecutorService executor) {
		this.app = app;
		this.applicationVersion = applicationVersion;
		this.localization = localization;
		this.settings = settings;
		this.semVerComparator = semVerComparator;
		this.executor = executor;
	}

	@FXML
	private Node checkForUpdatesContainer;

	@FXML
	private Label checkForUpdatesStatus;

	@FXML
	private ProgressIndicator checkForUpdatesIndicator;

	@FXML
	private Hyperlink updateLink;

	@FXML
	private VBox root;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (areUpdatesManagedExternally()) {
			checkForUpdatesContainer.setVisible(false);
		} else if (!settings.askedForUpdateCheck().get()) {
			this.askForUpdateCheck();
		} else if (settings.checkForUpdates().get()) {
			this.checkForUpdates();
		}
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	// ****************************************
	// Check for updates
	// ****************************************

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

	private void askForUpdateCheck() {
		Tasks.create(() -> {}).onSuccess(() -> {
			Optional<ButtonType> result = buildYesNoDialog(
					localization.getString("welcome.askForUpdateCheck.dialog.title"),
					localization.getString("welcome.askForUpdateCheck.dialog.header"),
					localization.getString("welcome.askForUpdateCheck.dialog.content"),
					ButtonType.YES).showAndWait();
			if (result.isPresent()) {
				settings.askedForUpdateCheck().set(true);
				settings.checkForUpdates().set(result.get().equals(ButtonType.YES));
			}
			if (settings.checkForUpdates().get()) {
				this.checkForUpdates();
			}
		}).scheduleOnce(executor, 1, TimeUnit.SECONDS);
	}

	private void checkForUpdates() {
		checkForUpdatesStatus.setText(localization.getString("welcome.checkForUpdates.label.currentlyChecking"));
		checkForUpdatesIndicator.setVisible(true);
		Tasks.create(() -> {
			String userAgent = String.format("Cryptomator VersionChecker/%s %s %s (%s)", applicationVersion.orElse("SNAPSHOT"), SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);
			URL url = URI.create("https://api.cryptomator.org/updates/latestVersion.json").toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.addRequestProperty("User-Agent", userAgent);
			conn.connect();
			try {
				if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					return Optional.<byte[]>empty();
				}
				try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					in.transferTo(out);
					return Optional.of(out.toByteArray());
				}
			} finally {
				conn.disconnect();
			}
		}).onSuccess(response -> {
			response.ifPresent(bytes -> {
				Gson gson = new GsonBuilder().setLenient().create();
				String json = new String(bytes, StandardCharsets.UTF_8);
				Map<String, String> map = gson.fromJson(json, new TypeToken<Map<String, String>>() {
				}.getType());
				if (map != null) {
					this.compareVersions(map);
				}
			});
		}).onError(Exception.class, e -> {
			LOG.warn("Error checking for updates", e);
		}).andFinally(() -> {
			checkForUpdatesStatus.setText("");
			checkForUpdatesIndicator.setVisible(false);
		}).runOnce(executor);
	}

	private void compareVersions(final Map<String, String> latestVersions) {
		assert Platform.isFxApplicationThread();
		final String latestVersion;
		if (SystemUtils.IS_OS_MAC_OSX) {
			latestVersion = latestVersions.get("mac");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			latestVersion = latestVersions.get("win");
		} else if (SystemUtils.IS_OS_LINUX) {
			latestVersion = latestVersions.get("linux");
		} else {
			// no version check possible on unsupported OS
			return;
		}
		final String currentVersion = applicationVersion.orElse(null);
		LOG.info("Current version: {}, lastest version: {}", currentVersion, latestVersion);
		if (currentVersion != null && semVerComparator.compare(currentVersion, latestVersion) < 0) {
			final String msg = String.format(localization.getString("welcome.newVersionMessage"), latestVersion, currentVersion);
			this.updateLink.setText(msg);
			this.updateLink.setVisible(true);
			this.updateLink.setDisable(false);
		}
	}

	@FXML
	public void didClickUpdateLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/");
	}

}
