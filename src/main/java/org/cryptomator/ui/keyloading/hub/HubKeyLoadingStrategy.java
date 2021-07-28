package org.cryptomator.ui.keyloading.hub;

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
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoading
public class HubKeyLoadingStrategy implements KeyLoadingStrategy {

	static final String SCHEME_HUB_HTTP = "hub+http";
	static final String SCHEME_HUB_HTTPS = "hub+https";
	private static final String SCHEME_HTTP = "http";
	private static final String SCHEME_HTTPS = "https";

	private final Vault vault;
	private final Stage window;
	private final Lazy<Scene> p12LoadingScene;
	private final UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock;
	private final AtomicReference<KeyPair> keyPairRef;

	@Inject
	public HubKeyLoadingStrategy(@KeyLoading Vault vault, @KeyLoading Stage window, @FxmlScene(FxmlFile.HUB_P12) Lazy<Scene> p12LoadingScene, UserInteractionLock<HubKeyLoadingModule.P12KeyLoading> p12LoadingLock, AtomicReference<KeyPair> keyPairRef) {
		this.vault = vault;
		this.window = window;
		this.p12LoadingScene = p12LoadingScene;
		this.p12LoadingLock = p12LoadingLock;
		this.keyPairRef = keyPairRef;
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		return switch (keyId.getScheme().toLowerCase()) {
			case SCHEME_HUB_HTTP -> loadKey(keyId, SCHEME_HTTP);
			case SCHEME_HUB_HTTPS -> loadKey(keyId, SCHEME_HTTPS);
			default -> throw new IllegalArgumentException("Only supports keys with schemes " + SCHEME_HUB_HTTP + " or " + SCHEME_HUB_HTTPS);
		};
	}

	private Masterkey loadKey(URI keyId, String adjustedScheme) {
		try {
			var foo = new URI(adjustedScheme, keyId.getSchemeSpecificPart(), keyId.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI known to be valid, if old URI was valid", e);
		}

		try {
			loadP12();
			LOG.info("keypair loaded {}", keyPairRef.get().getPublic());
			throw new UnlockCancelledException("not yet implemented"); // TODO
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Loading interrupted", e);
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
