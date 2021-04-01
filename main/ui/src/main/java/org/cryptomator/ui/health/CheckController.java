package org.cryptomator.ui.health;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class CheckController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckController.class);

	private final Vault vault;
	private final Stage window;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final SecureRandom csprng;

	@Inject
	public CheckController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vault = vault;
		this.window = window;
		this.masterkey = Objects.requireNonNull(masterkeyRef.get());
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.csprng = csprng;
	}

	@FXML
	public void runCheck() {
		// TODO run in background task...
		try (var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkey)) {
			List<DiagnosticResult> results = new ArrayList<>();
			HealthCheck.allChecks().forEach(c -> {
				LOG.info("Running check {}...", c.identifier());
				results.addAll(c.check(vault.getPath(), vaultConfig, masterkey, cryptor));
			});
			results.forEach(r -> {
				LOG.info("Result: {}", r);
			});
		}
	}

	public VaultConfig getVaultConfig() {
		return vaultConfig;
	}
}
