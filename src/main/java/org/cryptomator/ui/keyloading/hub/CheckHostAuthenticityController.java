package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@KeyLoadingScoped
public class CheckHostAuthenticityController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckHostAuthenticityController.class);

	private final Stage window;
	private final HubConfig hubConfig;
	private final Lazy<Scene> authFlowScene;
	private final CompletableFuture<ReceivedKey> result;
	private final Settings settings;
	private final Set<String> hostnames;

	@FXML
	private ListView<String> hostnamesList;

	@Inject
	public CheckHostAuthenticityController(@KeyLoading Stage window, HubConfig hubConfig, @FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene, CompletableFuture<ReceivedKey> result, Settings settings) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.authFlowScene = authFlowScene;
		this.result = result;
		this.settings = settings;
		this.hostnames = new HashSet<>();
	}

	@FXML
	public void initialize() {
		var authUri = URI.create(hubConfig.authEndpoint);
		var tokenUri = URI.create(hubConfig.tokenEndpoint);
		var apiBaseUri = hubConfig.getApiBaseUrl();
		var webappBaseUri = hubConfig.getWebappBaseUrl();

		if (!isConsistentHubConfig()) {
			LOG.warn("Inconsistent hub config detected. Denying access to protect the user.");
			Platform.runLater(this::deny);
		} else if (configContainsAllowedHosts()) {
			trust();
		} else if (Boolean.getBoolean("cryptomator.allowUnknownHubHosts")) {
			hostnames.addAll(List.of(authUri.getAuthority(), tokenUri.getAuthority(), apiBaseUri.getAuthority(), webappBaseUri.getAuthority()));
			hostnamesList.getItems().addAll(hostnames);
		} else {
			LOG.warn("Cryptomator is not allowed to connect to {}. Check your cryptomator.allowedHubHosts config.", webappBaseUri);
			Platform.runLater(this::deny);
		}
	}

	@FXML
	public void trust() {
		settings.trustedHosts.addAll(hostnames);
		window.setScene(authFlowScene.get());
	}

	@FXML
	public void deny() {
		result.cancel(true);
		window.close(); // TODO: show "denied" scene with explanation and "learn more" link to documentation
	}

	private boolean isConsistentHubConfig() {
		//hub endpoints are consistent
		//apiBaseURL.host == deviceUrl.host == authSuccessUrl.host == authErrorUrl.host
		var expectedHubHubHost = URI.create(hubConfig.authSuccessUrl).getHost(); //apiBaseURL could be null! hence, the authSuccessUrl
		if (hubConfig.apiBaseUrl != null && hasDifferentHost(hubConfig.apiBaseUrl, expectedHubHubHost)) {
			return false;
		}
		if (hasDifferentHost(hubConfig.devicesResourceUrl, expectedHubHubHost)) {
			return false;
		}
		if (hasDifferentHost(hubConfig.authErrorUrl, expectedHubHubHost)) {
			return false;
		}

		//auth endpoints are consistent
		//authUrl.host == tokenUrl.host
		var expectedHubAuthHost = URI.create(hubConfig.authEndpoint).getHost();
		if (hasDifferentHost(hubConfig.tokenEndpoint, expectedHubAuthHost)) {
			return false;
		}
		return true;
	}

	private boolean configContainsAllowedHosts() {
		var allowedHubHostsString = System.getProperty("cryptomator.allowedHubHosts", "");
		//https://example.com,http://foo.bar:3333
		var allowedHubHosts = Arrays.stream(allowedHubHostsString.split(",")).map(String::trim).toList(); //foo.bar

		var expectedHubHubAuthorities = URI.create(hubConfig.authSuccessUrl).getAuthority(); //apiBaseURL could be null! hence, the authSuccessUrl
		var expectedHubAuthAuthorities = URI.create(hubConfig.authEndpoint).getAuthority();
		//are the hosts also allowed?
		var isHubHubHostAllowed = allowedHubHosts.stream().anyMatch(expectedHubHubAuthorities::equals);
		var isHubAuthHostAllowed = allowedHubHosts.stream().anyMatch(expectedHubAuthAuthorities::equals);
		return isHubAuthHostAllowed && isHubHubHostAllowed;
	}

	private boolean hasDifferentHost(String uri, String host) {
		try {
			return !URI.create(uri).getHost().equals(host);
		} catch (IllegalArgumentException e) {
			return true;
		}
	}

}
