package org.cryptomator.ui.health;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.cryptofs.VaultKeyInvalidException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@HealthCheckScoped
public class StartController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(StartController.class);

	private final Stage window;
	private final Optional<VaultConfig.UnverifiedVaultConfig> unverifiedVaultConfig;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final ExecutorService executor;

	@Inject
	public StartController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, @HealthCheckWindow KeyLoadingStrategy keyLoadingStrategy, ExecutorService executor) {
		this.window = window;
		this.unverifiedVaultConfig = vault.getUnverifiedVaultConfig();
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.executor = executor;
	}

	@FXML
	public void close() {
		LOG.trace("StartController.close()");
		window.close();
	}

	@FXML
	public void next() {
		LOG.trace("StartController.next()");
		executor.submit(this::loadKey);
	}

	private void loadKey() {
		assert !Platform.isFxApplicationThread();
		try (var masterkey = keyLoadingStrategy.masterkeyLoader().loadKey(unverifiedVaultConfig.orElseThrow().getKeyId())) {
			var clone = masterkey.clone(); // original key will get destroyed
			Platform.runLater(() -> loadedKey(clone));
		} catch (MasterkeyLoadingFailedException e) {
			if (keyLoadingStrategy.recoverFromException(e)) {
				// retry
				loadKey();
			} else {
				Platform.runLater(() -> loadingKeyFailed(e));
			}
		}
	}

	private void loadedKey(Masterkey masterkey) {
		assert unverifiedVaultConfig.isPresent();
		var unverifiedCfg = unverifiedVaultConfig.get();
		try {
			var verifiedCfg = unverifiedCfg.verify(masterkey.getEncoded(), unverifiedCfg.allegedVaultVersion());
			LOG.info("Verified vault config with cipher {}", verifiedCfg.getCipherCombo());
		} catch (VaultKeyInvalidException e) {
			LOG.error("Invalid key");
			// TODO show error screen
		} catch (VaultConfigLoadException e) {
			LOG.error("Failed to verify vault config", e);
			// TODO show error screen
		} finally {
			masterkey.destroy();
		}
	}

	private void loadingKeyFailed(MasterkeyLoadingFailedException e) {
		if (e instanceof UnlockCancelledException) {
			// ok
		} else {
			LOG.error("Failed to load key.", e);
			// TODO show error screen
		}
	}

	public boolean isInvalidConfig() {
		return unverifiedVaultConfig.isEmpty();
	}

	public int getVaultVersion() {
		return unverifiedVaultConfig
				.map(VaultConfig.UnverifiedVaultConfig::allegedVaultVersion)
				.orElse(-1);
	}

	public String getKeyId() {
		return unverifiedVaultConfig
				.map(VaultConfig.UnverifiedVaultConfig::getKeyId)
				.map(URI::toString)
				.orElse(null);
	}
}
