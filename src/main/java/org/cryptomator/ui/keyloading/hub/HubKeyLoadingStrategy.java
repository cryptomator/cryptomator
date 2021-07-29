package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoading
public class HubKeyLoadingStrategy implements KeyLoadingStrategy {

	private static final String SCHEME_PREFIX = "hub+";
	static final String SCHEME_HUB_HTTP = SCHEME_PREFIX + "http";
	static final String SCHEME_HUB_HTTPS = SCHEME_PREFIX + "https";

	private final Application application;
	private final ExecutorService executor;
	private final Vault vault;
	private final Stage window;
	private final Lazy<Scene> p12LoadingScene;
	private final UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock;
	private final AtomicReference<KeyPair> keyPairRef;

	@Inject
	public HubKeyLoadingStrategy(Application application, ExecutorService executor, @KeyLoading Vault vault, @KeyLoading Stage window, @FxmlScene(FxmlFile.HUB_P12) Lazy<Scene> p12LoadingScene, UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock, AtomicReference<KeyPair> keyPairRef) {
		this.application = application;
		this.executor = executor;
		this.vault = vault;
		this.window = window;
		this.p12LoadingScene = p12LoadingScene;
		this.p12LoadingLock = p12LoadingLock;
		this.keyPairRef = keyPairRef;
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(keyId.getScheme().startsWith(SCHEME_PREFIX));
		try {
			loadP12();
			LOG.info("keypair loaded {}", keyPairRef.get().getPublic());
			var task = new ReceiveEncryptedMasterkeyTask(redirectUri -> {
				openBrowser(keyId, redirectUri);
			});
			executor.submit(task);
			throw new UnlockCancelledException("not yet implemented"); // TODO
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Loading interrupted", e);
		}
	}

	private void openBrowser(URI keyId, URI redirectUri) {
		Preconditions.checkArgument(keyId.getScheme().startsWith(SCHEME_PREFIX));
		var httpScheme = keyId.getScheme().substring(SCHEME_PREFIX.length());
		var redirectParam = "redirect_uri="+ URLEncoder.encode(redirectUri.toString(), StandardCharsets.US_ASCII);
		try {
			var uri = new URI(httpScheme, keyId.getAuthority(), keyId.getPath(), redirectParam, null);
			application.getHostServices().showDocument(uri.toString());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from params known to be valid", e);
		}
	}

	private HubKeyLoadingModule.P12KeyLoading loadP12() throws InterruptedException {
		Platform.runLater(() -> {
			window.setScene(p12LoadingScene.get());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
		});
		return p12LoadingLock.awaitInteraction();
	}

}
