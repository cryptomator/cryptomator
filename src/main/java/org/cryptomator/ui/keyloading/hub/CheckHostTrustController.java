package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
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
	private static final String DESCRIPTION_SINGULAR_KEY = "hub.checkHostTrust.description.ask";
	private static final String DESCRIPTION_PLURAL_KEY = "hub.checkHostTrust.description.ask.plural";
	private static final String COPY_TOOLTIP_KEY = "hub.checkHostTrust.copyBtn.tooltip";
	private static final String TRUSTED_CRYPTOMATOR_CLOUD_DOMAIN = ".cryptomator.cloud";
	private static final Duration COPIED_INDICATION_DURATION = Duration.seconds(2);

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
	private final StringProperty descriptionLabel;

	@FXML
	private VBox hostnamesBox;

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
		this.descriptionLabel = new SimpleStringProperty("");
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
		hostnamesBox.getChildren().clear();
		for (var hostname : hostnames) {
			hostnamesBox.getChildren().add(createHostnameRow(hostname));
		}
		var plural = hostnames.size() > 1;
		messageLabel.set(resourceBundle.getString(plural ? ASK_PLURAL_KEY : ASK_SINGULAR_KEY));
		descriptionLabel.set(resourceBundle.getString(plural ? DESCRIPTION_PLURAL_KEY : DESCRIPTION_SINGULAR_KEY));
	}

	private Node createHostnameRow(String hostname) {
		var label = new Label(hostname);
		label.setWrapText(true);
		HBox.setHgrow(label, Priority.ALWAYS);

		var icon = new FontAwesome5IconView();
		icon.setGlyph(FontAwesome5Icon.COPY);
		var copyLink = new Hyperlink(null, icon);
		copyLink.setTooltip(new Tooltip(resourceBundle.getString(COPY_TOOLTIP_KEY)));
		copyLink.setAccessibleText(resourceBundle.getString(COPY_TOOLTIP_KEY));
		copyLink.setOnAction(_ -> copyToClipboard(hostname, icon));

		var row = new HBox(6, label, copyLink);
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	private void copyToClipboard(String hostname, FontAwesome5IconView icon) {
		var clipboardContent = new ClipboardContent();
		clipboardContent.putString(hostname);
		Clipboard.getSystemClipboard().setContent(clipboardContent);

		icon.setGlyph(FontAwesome5Icon.CHECK);
		var resetIcon = new PauseTransition(COPIED_INDICATION_DURATION);
		resetIcon.setOnFinished(_ -> icon.setGlyph(FontAwesome5Icon.COPY));
		resetIcon.play();
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
		if (uri.getPort() == -1) {
			return "%s://%s".formatted(uri.getScheme(), uri.getHost());
		} else {
			return "%s://%s:%s".formatted(uri.getScheme(), uri.getHost(), uri.getPort());
		}
	}

	//--- JavaFX property getter & setter
	public StringProperty messageLabelProperty() {
		return messageLabel;
	}

	public String getMessageLabel() {
		return messageLabel.get();
	}

	public StringProperty descriptionLabelProperty() {
		return descriptionLabel;
	}

	public String getDescriptionLabel() {
		return descriptionLabel.get();
	}

}
