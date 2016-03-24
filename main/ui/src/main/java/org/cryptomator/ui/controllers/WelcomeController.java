/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

@Singleton
public class WelcomeController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);

	private final Application app;
	private final Settings settings;
	private final Comparator<String> semVerComparator;
	private final ExecutorService executor;

	@Inject
	public WelcomeController(Application app, Localization localization, Settings settings, @Named("SemVer") Comparator<String> semVerComparator, ExecutorService executor) {
		super(localization);
		this.app = app;
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

	@Override
	public void initialize() {
		if (areUpdatesManagedExternally()) {
			checkForUpdatesContainer.setVisible(false);
		} else if (settings.isCheckForUpdatesEnabled()) {
			executor.execute(this::checkForUpdates);
		}
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/welcome.fxml");
	}

	// ****************************************
	// Check for updates
	// ****************************************

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

	private void checkForUpdates() {
		Platform.runLater(() -> {
			checkForUpdatesStatus.setText(localization.getString("welcome.checkForUpdates.label.currentlyChecking"));
			checkForUpdatesIndicator.setVisible(true);
		});
		final HttpClient client = new HttpClient();
		final HttpMethod method = new GetMethod("https://cryptomator.org/downloads/latestVersion.json");
		client.getParams().setParameter(HttpClientParams.USER_AGENT, "Cryptomator VersionChecker/" + applicationVersion().orElse("SNAPSHOT"));
		client.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		client.getParams().setConnectionManagerTimeout(5000);
		try {
			client.executeMethod(method);
			final InputStream responseBodyStream = method.getResponseBodyAsStream();
			if (method.getStatusCode() == HttpStatus.SC_OK && responseBodyStream != null) {
				final byte[] responseData = IOUtils.toByteArray(responseBodyStream);
				final ObjectMapper mapper = new ObjectMapper();
				final Map<String, String> map = mapper.readValue(responseData, new TypeReference<HashMap<String, String>>() {
				});
				if (map != null) {
					this.compareVersions(map);
				}
			}
		} catch (IOException e) {
			// no error handling required. Maybe next time the version check is successful.
		} finally {
			Platform.runLater(() -> {
				checkForUpdatesStatus.setText("");
				checkForUpdatesIndicator.setVisible(false);
			});
		}
	}

	private Optional<String> applicationVersion() {
		return Optional.ofNullable(getClass().getPackage().getImplementationVersion());
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
		final String currentVersion = applicationVersion().orElse(null);
		LOG.debug("Current version: {}, lastest version: {}", currentVersion, latestVersion);
		if (currentVersion != null && semVerComparator.compare(currentVersion, latestVersion) < 0) {
			final String msg = String.format(localization.getString("welcome.newVersionMessage"), latestVersion, currentVersion);
			Platform.runLater(() -> {
				this.updateLink.setText(msg);
				this.updateLink.setVisible(true);
			});
		}
	}

	@FXML
	public void didClickUpdateLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/#download");
	}

}
