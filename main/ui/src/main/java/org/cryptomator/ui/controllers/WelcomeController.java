/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.util.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

@Singleton
public class WelcomeController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);

	private final Application app;
	private final Optional<String> applicationVersion;
	private final Localization localization;
	private final Settings settings;
	private final Comparator<String> semVerComparator;
	private final AsyncTaskService asyncTaskService;

	@Inject
	public WelcomeController(Application app, @Named("applicationVersion") Optional<String> applicationVersion, Localization localization, Settings settings, @Named("SemVer") Comparator<String> semVerComparator,
			AsyncTaskService asyncTaskService) {
		this.app = app;
		this.applicationVersion = applicationVersion;
		this.localization = localization;
		this.settings = settings;
		this.semVerComparator = semVerComparator;
		this.asyncTaskService = asyncTaskService;
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

	private void checkForUpdates() {
		checkForUpdatesStatus.setText(localization.getString("welcome.checkForUpdates.label.currentlyChecking"));
		checkForUpdatesIndicator.setVisible(true);
		asyncTaskService.asyncTaskOf(() -> {
			RequestConfig requestConfig = RequestConfig.custom() //
					.setConnectTimeout(5000) //
					.setConnectionRequestTimeout(5000) //
					.setSocketTimeout(5000) //
					.build();
			String userAgent = String.format("Cryptomator VersionChecker/%s %s %s (%s)", applicationVersion.orElse("SNAPSHOT"), SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);
			HttpClientBuilder httpClientBuilder = HttpClients.custom() //
					.disableCookieManagement() //
					.setDefaultRequestConfig(requestConfig) //
					.setUserAgent(userAgent);
			LOG.debug("Checking for updates...");
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				HttpGet request = new HttpGet("https://api.cryptomator.org/updates/latestVersion.json");
				try (CloseableHttpResponse response = client.execute(request)) {
					if (response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
						try (InputStream in = response.getEntity().getContent()) {
							Gson gson = new GsonBuilder().setLenient().create();
							Reader utf8Reader = new InputStreamReader(in, StandardCharsets.UTF_8);
							Map<String, String> map = gson.fromJson(utf8Reader, new TypeToken<Map<String, String>>() {
							}.getType());
							if (map != null) {
								this.compareVersions(map);
							}
						}
					}
				}
			}
		}).andFinally(() -> {
			checkForUpdatesStatus.setText("");
			checkForUpdatesIndicator.setVisible(false);
		}).run();
	}

	private void compareVersions(final Map<String, String> latestVersions) {
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
			Platform.runLater(() -> {
				this.updateLink.setText(msg);
				this.updateLink.setVisible(true);
				this.updateLink.setDisable(false);
			});
		}
	}

	@FXML
	public void didClickUpdateLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/");
	}

}
