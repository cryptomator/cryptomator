package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.Environment;
import org.cryptomator.common.FilesystemOwnerSupplier;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.keychain.NoKeychainAccessProviderException;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoading
public class HubKeyLoadingStrategy implements KeyLoadingStrategy, FilesystemOwnerSupplier {

	public static final String SCHEME_PREFIX = "hub+";
	public static final String SCHEME_HUB_HTTP = SCHEME_PREFIX + "http";
	public static final String SCHEME_HUB_HTTPS = SCHEME_PREFIX + "https";

	private final Stage window;
	private final KeychainManager keychainManager;
	private final AtomicReference<String> fsOwnerId;
	private final HubConfig hubConfig;
	private final Lazy<Scene> authFlowScene;
	private final Lazy<Scene> noKeychainScene;
	private final CompletableFuture<ReceivedKey> result;
	private final DeviceKey deviceKey;

	@Inject
	public HubKeyLoadingStrategy(@KeyLoading Stage window, @FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene, @FxmlScene(FxmlFile.HUB_NO_KEYCHAIN) Lazy<Scene> noKeychainScene, CompletableFuture<ReceivedKey> result, DeviceKey deviceKey, KeychainManager keychainManager, @Named("windowTitle") String windowTitle, @Named("filesystemOwnerId") AtomicReference<String> fsOwnerId, HubConfig hubConfig) {
		this.window = window;
		this.keychainManager = keychainManager;
		this.fsOwnerId = fsOwnerId;
		this.hubConfig = hubConfig;
		window.setTitle(windowTitle);
		window.setOnCloseRequest(_ -> result.cancel(true));
		this.authFlowScene = authFlowScene;
		this.noKeychainScene = noKeychainScene;
		this.result = result;
		this.deviceKey = deviceKey;
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(keyId.getScheme().startsWith(SCHEME_PREFIX));
		try {
			if (!keychainManager.isSupported()) {
				throw new NoKeychainAccessProviderException();
			}
			var keypair = deviceKey.get();

			//check hub config
			isConsistentHubConfig();
			if (configContainsAllowedHosts()) {
				showWindow(authFlowScene);
				var jwe = result.get();
				return jwe.decryptMasterkey(keypair.getPrivate());
			} else {
				var showUnknownHubHostDialog = Environment.getInstance().allowUnknownHubHosts();
				//TODO show window
				throw new MasterkeyLoadingFailedException("Unknown hub host in vault config");
			}

		} catch (NoKeychainAccessProviderException e) {
			showWindow(noKeychainScene);
			throw new UnlockCancelledException("Unlock canceled due to missing prerequisites", e);
		} catch (DeviceKey.DeviceKeyRetrievalException e) {
			throw new MasterkeyLoadingFailedException("Failed to load keypair", e);
		} catch (CancellationException e) {
			throw new UnlockCancelledException("User cancelled auth workflow", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Loading interrupted", e);
		} catch (ExecutionException e) {
			throw new MasterkeyLoadingFailedException("Failed to retrieve key", e);
		}
	}

	private void isConsistentHubConfig() {
		//hub endpoints are consistent
		//apiBaseURL.host == deviceUrl.host == authSuccessUrl.host == authErrorUrl.host
		var expectedHubHubHost = URI.create(hubConfig.authSuccessUrl).getHost(); //apiBaseURL could be null! hence, the authSuccessUrl
		if (hubConfig.apiBaseUrl != null && hasDifferentHost(hubConfig.apiBaseUrl, expectedHubHubHost)) {
			//throw
		}
		if (hasDifferentHost(hubConfig.devicesResourceUrl, expectedHubHubHost)) {
			//throw
		}
		if (hasDifferentHost(hubConfig.authErrorUrl, expectedHubHubHost)) {
			//throw
		}

		//auth endpoints are consistent
		//authUrl.host == tokenUrl.host
		var expectedHubAuthHost = URI.create(hubConfig.authEndpoint).getHost();
		if (hasDifferentHost(hubConfig.tokenEndpoint, expectedHubAuthHost)) {
			//throw
		}
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

	private void showWindow(Lazy<Scene> scene) {
		Platform.runLater(() -> {
			window.setScene(scene.get());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
		});
	}

	@Override
	public String getOwner() {
		var name = fsOwnerId.get();
		if (name == null) {
			throw new IllegalStateException("Owner is not yet determined");
		}
		return name;
	}

}
