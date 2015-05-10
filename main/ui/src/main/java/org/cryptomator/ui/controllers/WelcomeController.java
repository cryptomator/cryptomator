/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WelcomeController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);

	@FXML
	private ImageView botImageView;

	@FXML
	private Hyperlink updateLink;

	private final Application app;
	private final Comparator<String> semVerComparator;
	private final ExecutorService executor;
	private ResourceBundle rb;

	@Inject
	public WelcomeController(Application app, @Named("SemVer") Comparator<String> semVerComparator, ExecutorService executor) {
		this.app = app;
		this.semVerComparator = semVerComparator;
		this.executor = executor;
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;
		this.botImageView.setImage(new Image(WelcomeController.class.getResource("/bot_welcome.png").toString()));
		executor.execute(this::checkForUpdates);
	}

	private void checkForUpdates() {
		final HttpClient client = new HttpClient();
		final HttpMethod method = new GetMethod("https://cryptomator.org/downloads/latestVersion.json");
		client.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		client.getParams().setConnectionManagerTimeout(5000);
		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				final byte[] responseData = method.getResponseBody();
				final ObjectMapper mapper = new ObjectMapper();
				final Map<String, String> map = mapper.readValue(responseData, new TypeReference<HashMap<String, String>>() {
				});
				this.compareVersions(map);
			}
		} catch (IOException e) {
			// no error handling required. Maybe next time the version check is successful.
		}
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
		final String currentVersion = WelcomeController.class.getPackage().getImplementationVersion();
		LOG.debug("Current version: {}, lastest version: {}", currentVersion, latestVersion);
		if (currentVersion != null && semVerComparator.compare(currentVersion, latestVersion) < 0) {
			final String msg = String.format(rb.getString("welcome.newVersionMessage"), latestVersion, currentVersion);
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
