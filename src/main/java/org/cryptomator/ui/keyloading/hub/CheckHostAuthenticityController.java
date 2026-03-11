package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@KeyLoadingScoped
public class CheckHostAuthenticityController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckHostAuthenticityController.class);
	private static final String MESSAGE_SINGULAR_KEY = "hub.checkHostAuthenticity.message";
	private static final String MESSAGE_PLURAL_KEY = "hub.checkHostAuthenticity.message.plural";

	private final Stage window;
	private final HubConfig hubConfig;
	private final Lazy<Scene> authFlowScene;
	private final Lazy<Scene> unauthorizedHostScene;
	private final CompletableFuture<ReceivedKey> result;
	private final Settings settings;
	private final ResourceBundle resourceBundle;
	private final Set<String> hostnames;

	@FXML
	private Label messageLabel;

	@FXML
	private TextFlow hostnamesFlow;

	@Inject
	public CheckHostAuthenticityController(@KeyLoading Stage window, HubConfig hubConfig, @FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene, @FxmlScene(FxmlFile.HUB_UNAUTHORIZED_HOST) Lazy<Scene> unauthorizedHostScene, CompletableFuture<ReceivedKey> result, Settings settings, ResourceBundle resourceBundle) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.authFlowScene = authFlowScene;
		this.unauthorizedHostScene = unauthorizedHostScene;
		this.result = result;
		this.settings = settings;
		this.resourceBundle = resourceBundle;
		this.hostnames = new HashSet<>();
	}

	@FXML
	public void initialize() {
		if (!isConsistentHubConfig()) {
			LOG.warn("Inconsistent hub config detected. Denying access to protect the user.");
			Platform.runLater(this::deny);
		} else if (configContainsAllowedHosts()) {
			trust();
		} else if (Boolean.getBoolean("cryptomator.allowUnknownHubHosts") && containsAllowedHosts(settings.trustedHosts)) {
			trust();
		} else if (Boolean.getBoolean("cryptomator.allowUnknownHubHosts")) {
			hostnames.add(getAuthority(hubConfig.getApiBaseUrl()));
			hostnames.add(getAuthority(hubConfig.authEndpoint));
			renderHostnames();
		} else {
			LOG.warn("Cryptomator is not allowed to connect to {}. Check your cryptomator.allowedHubHosts config.", getAuthority(hubConfig.getApiBaseUrl()));
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
		window.setScene(unauthorizedHostScene.get());
	}

	private void renderHostnames() {
		hostnamesFlow.getChildren().clear();
		hostnames.stream().sorted().forEach(hostname -> hostnamesFlow.getChildren().add(new Text(hostname + System.lineSeparator())));
		var messageKey = hostnames.size() > 1 ? MESSAGE_PLURAL_KEY : MESSAGE_SINGULAR_KEY;
		messageLabel.setText(resourceBundle.getString(messageKey));
	}

	private boolean isConsistentHubConfig() {
		var canonicalHubHost = getAuthority(hubConfig.getApiBaseUrl());
		var canonicalAuthHost = getAuthority(hubConfig.authEndpoint);

		// apiBaseURL.host == deviceUrl.host == authSuccessUrl.host == authErrorUrl.host
		return (hubConfig.apiBaseUrl == null || getAuthority(hubConfig.apiBaseUrl).equals(canonicalHubHost)) //
				&& getAuthority(hubConfig.devicesResourceUrl).equals(canonicalHubHost) //
				&& getAuthority(hubConfig.authSuccessUrl).equals(canonicalHubHost) //
				&& getAuthority(hubConfig.authErrorUrl).equals(canonicalHubHost) //
				// authUrl.host == tokenUrl.host:
				&& getAuthority(hubConfig.tokenEndpoint).equals(canonicalAuthHost);
	}

	private boolean configContainsAllowedHosts() {
		var allowedHubHostsString = System.getProperty("cryptomator.allowedHubHosts", ""); // https://example.com,http://foo.bar:3333
		var allowedHubHosts = Arrays.stream(allowedHubHostsString.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
		return containsAllowedHosts(allowedHubHosts);
	}

	@VisibleForTesting
	boolean containsAllowedHosts(Set<String> allowedHubHosts) {
		var canonicalHubHost = getAuthority(hubConfig.getApiBaseUrl());
		var canonicalAuthHost = getAuthority(hubConfig.authEndpoint);
		return allowedHubHosts.contains(canonicalHubHost) && allowedHubHosts.contains(canonicalAuthHost);
	}

	public static String getAuthority(String string) {
		return getAuthority(URI.create(string));
	}

	public static String getAuthority(URI uri) {
		return switch (uri.getPort()) {
			case -1 -> "%s://%s".formatted(uri.getScheme(), uri.getHost());
			case 80 -> "http://%s".formatted(uri.getHost());
			case 443 -> "https://%s".formatted(uri.getHost());
			default -> "%s://%s:%s".formatted(uri.getScheme(), uri.getHost(), uri.getPort());
		};
	}

}
