package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
import org.cryptomator.common.Environment;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import java.net.URI;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

@KeyLoadingScoped
public class CheckHostTrustController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckHostTrustController.class);
	private static final String CHECK_KEY = "hub.checkHostTrust.message.check";
	private static final String ASK_SINGULAR_KEY = "hub.checkHostTrust.message.ask";
	private static final String ASK_PLURAL_KEY = "hub.checkHostTrust.message.ask.plural";
	private static final String TRUSTED_CRYPTOMATOR_CLOUD_DOMAIN = ".cryptomator.cloud";

	private final Stage window;
	private final HubConfig hubConfig;
	private final URI canonicalHubUri;
	private final URI canonicalAuthUri;
	private final Lazy<Scene> authFlowScene;
	private final Lazy<Scene> untrustedHostScene;
	private final CompletableFuture<ReceivedKey> result;
	private final Settings settings;
	private final Environment env;
	private final ResourceBundle resourceBundle;
	private final SortedSet<String> hostnames;
	private final StringProperty messageLabel;

	@FXML
	private TextFlow hostnamesFlow;

	@Inject
	public CheckHostTrustController(@KeyLoading Stage window, //
									HubConfig hubConfig, //
									@FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene, //
									@FxmlScene(FxmlFile.HUB_UNTRUSTED_HOST) Lazy<Scene> untrustedHostScene, //
									CompletableFuture<ReceivedKey> result, //
									Settings settings, //
									Environment env, //
									ResourceBundle resourceBundle) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.canonicalHubUri = hubConfig.getApiBaseUrl();
		this.canonicalAuthUri = URI.create(hubConfig.authEndpoint);
		this.authFlowScene = authFlowScene;
		this.untrustedHostScene = untrustedHostScene;
		this.result = result;
		this.settings = settings;
		this.env = env;
		this.resourceBundle = resourceBundle;
		this.hostnames = new TreeSet<>();
		this.messageLabel = new SimpleStringProperty(resourceBundle.getString(CHECK_KEY));
	}

	@FXML
	public void initialize() {
		if (!isConsistentHubConfig()) {
			LOG.warn("Inconsistent hub config detected. Denying access to protect the user.");
			deny();
		} else if (isAllCryptomatorCloud() && !isAnyHttpHost()) {
			trust(); // trust *.cryptomator.cloud by default, domain is owned by Cryptomator maintainers
		} else if (containsAllowedHosts(env.hubAllowedHosts())) {
			trust(); // trust hosts explicitly allowlisted via system property
		} else if (isAnyHttpHost() && !isAllLocalhost()) {
			LOG.warn("Denying attempt to connect to hub instance via unencrypted HTTP.");
			deny(); // never trust http hosts except for local testing
		} else if (env.hubTrustOnFirstUse() && containsAllowedHosts(settings.trustedHosts)) {
			trust(); // trust hosts previously allowlisted by the user
		} else if (env.hubTrustOnFirstUse()) {
			hostnames.add(getAuthority(canonicalHubUri));
			hostnames.add(getAuthority(canonicalAuthUri));
			renderHostnames(); // ask user whether to trust these hosts
		} else {
			LOG.warn("Cryptomator is not allowed to connect to {}. Check your {} config.", getAuthority(canonicalHubUri), Environment.HUB_ALLOWED_HOSTS_PROP_NAME);
			deny();
		}
	}

	@FXML
	public void trust() {
		settings.trustedHosts.addAll(hostnames);
		Platform.runLater(() -> {
			window.setScene(authFlowScene.get());
		});
	}

	@FXML
	public void deny() {
		result.cancel(true);
		Platform.runLater(() -> {
			window.setScene(untrustedHostScene.get());
		});
	}

	private void renderHostnames() {
		hostnamesFlow.getChildren().clear();
		for (var hostname : hostnames) {
			hostnamesFlow.getChildren().add(new Text(hostname + System.lineSeparator()));
		}
		var messageKey = hostnames.size() > 1 ? ASK_PLURAL_KEY : ASK_SINGULAR_KEY;
		messageLabel.set(resourceBundle.getString(messageKey));
	}

	private boolean isConsistentHubConfig() {
		var canonicalHubAuthority = getAuthority(canonicalHubUri);
		var canonicalAuthAuthority = getAuthority(canonicalAuthUri);

		// apiBaseURL.host == deviceUrl.host == authSuccessUrl.host == authErrorUrl.host
		return (hubConfig.apiBaseUrl == null || getAuthority(hubConfig.apiBaseUrl).equals(canonicalHubAuthority)) //
				&& (hubConfig.devicesResourceUrl == null || getAuthority(hubConfig.devicesResourceUrl).equals(canonicalHubAuthority)) //
				&& getAuthority(hubConfig.authSuccessUrl).equals(canonicalHubAuthority) //
				&& getAuthority(hubConfig.authErrorUrl).equals(canonicalHubAuthority) //
				// authUrl.host == tokenUrl.host:
				&& getAuthority(hubConfig.tokenEndpoint).equals(canonicalAuthAuthority);
	}

	private boolean isAllCryptomatorCloud() {
		return canonicalHubUri.getHost().endsWith(TRUSTED_CRYPTOMATOR_CLOUD_DOMAIN) && canonicalAuthUri.getHost().endsWith(TRUSTED_CRYPTOMATOR_CLOUD_DOMAIN);
	}

	private boolean isAnyHttpHost() {
		return "http".equalsIgnoreCase(canonicalHubUri.getScheme()) || "http".equalsIgnoreCase(canonicalAuthUri.getScheme());
	}

	private boolean isAllLocalhost() {
		return "localhost".equalsIgnoreCase(canonicalHubUri.getHost()) && "localhost".equalsIgnoreCase(canonicalAuthUri.getHost());
	}

	@VisibleForTesting
	boolean containsAllowedHosts(Set<String> allowedHubHosts) {
		return allowedHubHosts.contains(getAuthority(canonicalHubUri)) && allowedHubHosts.contains(getAuthority(canonicalAuthUri));
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

	//--- JavaFX property getter & setter
	public StringProperty messageLabelProperty() {
		return messageLabel;
	}

	public String getMessageLabel() {
		return messageLabel.get();
	}

}
