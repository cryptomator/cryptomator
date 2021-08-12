package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.Destroyables;
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
import java.security.KeyPair;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoading
public class HubKeyLoadingStrategy implements KeyLoadingStrategy {

	private static final String SCHEME_PREFIX = "hub+";
	static final String SCHEME_HUB_HTTP = SCHEME_PREFIX + "http";
	static final String SCHEME_HUB_HTTPS = SCHEME_PREFIX + "https";

	private final Vault vault;
	private final Stage window;
	private final Lazy<Scene> p12LoadingScene;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> userInteraction;
	private final AtomicReference<KeyPair> keyPairRef;
	private final AtomicReference<EciesParams> eciesParams;

	@Inject
	public HubKeyLoadingStrategy(@KeyLoading Vault vault, @KeyLoading Stage window, @FxmlScene(FxmlFile.HUB_P12) Lazy<Scene> p12LoadingScene, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> userInteraction, AtomicReference<KeyPair> keyPairRef, AtomicReference<EciesParams> eciesParams) {
		this.vault = vault;
		this.window = window;
		this.p12LoadingScene = p12LoadingScene;
		this.userInteraction = userInteraction;
		this.keyPairRef = keyPairRef;
		this.eciesParams = eciesParams;
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(keyId.getScheme().startsWith(SCHEME_PREFIX));
		try {
			return switch (auth()) {
				case SUCCESS -> EciesHelper.decryptMasterkey(keyPairRef.get(), eciesParams.get());
				case FAILED -> throw new MasterkeyLoadingFailedException("failed to load keypair");
				case CANCELLED -> throw new UnlockCancelledException("User cancelled auth workflow");
			};
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Loading interrupted", e);
		}
	}

	@Override
	public void cleanup(boolean unlockedSuccessfully) {
		var keyPair = keyPairRef.getAndSet(null);
		if (keyPair != null) {
			Destroyables.destroySilently(keyPair.getPrivate());
		}
	}

	private HubKeyLoadingModule.HubLoadingResult auth() throws InterruptedException {
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
		return userInteraction.awaitInteraction();
	}

}
