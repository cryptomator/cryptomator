package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.Destroyables;
import org.cryptomator.cryptolib.common.MasterkeyHubAccess;
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

	private final Stage window;
	private final Lazy<Scene> authFlowScene;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> userInteraction;
	private final DeviceKey deviceKey;
	private final AtomicReference<EciesParams> eciesParams;

	@Inject
	public HubKeyLoadingStrategy(@KeyLoading Stage window, @FxmlScene(FxmlFile.HUB_AUTH_FLOW) Lazy<Scene> authFlowScene, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> userInteraction, DeviceKey deviceKey, AtomicReference<EciesParams> eciesParams) {
		this.window = window;
		this.authFlowScene = authFlowScene;
		this.userInteraction = userInteraction;
		this.deviceKey = deviceKey;
		this.eciesParams = eciesParams;
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(keyId.getScheme().startsWith(SCHEME_PREFIX));
		try {
			var keyPair = deviceKey.get();
			return switch (auth()) {
				case SUCCESS -> MasterkeyHubAccess.decryptMasterkey(keyPair.getPrivate(), eciesParams.get().m(), eciesParams.get().epk());
				case FAILED -> throw new MasterkeyLoadingFailedException("failed to load keypair");
				case CANCELLED -> throw new UnlockCancelledException("User cancelled auth workflow");
			};
		} catch (DeviceKey.DeviceKeyRetrievalException e) {
			throw new MasterkeyLoadingFailedException("Failed to create or load device key pair", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Loading interrupted", e);
		}
	}

	private HubKeyLoadingModule.HubLoadingResult auth() throws InterruptedException {
		Platform.runLater(() -> {
			window.setScene(authFlowScene.get());
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
