package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import dagger.Lazy;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@KeyLoading
public class HubKeyLoadingStrategy implements KeyLoadingStrategy {

	private static final String SCHEME_PREFIX = "hub+";
	public static final String SCHEME_HUB_HTTP = SCHEME_PREFIX + "http";
	public static final String SCHEME_HUB_HTTPS = SCHEME_PREFIX + "https";

	private final Stage window;
	private final KeychainManager keychainManager;
	private final Lazy<Scene> authFlowScene;
	private final Lazy<Scene> noKeychainScene;
	private final CompletableFuture<ReceivedKey> result;
	private final DeviceKey deviceKey;

	@Inject
	public HubKeyLoadingStrategy(@KeyLoading Stage window, @FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene, @FxmlScene(FxmlFile.HUB_NO_KEYCHAIN) Lazy<Scene> noKeychainScene, CompletableFuture<ReceivedKey> result, DeviceKey deviceKey, KeychainManager keychainManager, @Named("windowTitle") String windowTitle) {
		this.window = window;
		this.keychainManager = keychainManager;
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
			showWindow(authFlowScene);
			var jwe = result.get();
			return jwe.decryptMasterkey(keypair.getPrivate());
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

}
