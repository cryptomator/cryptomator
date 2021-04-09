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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class CheckController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckController.class);

	private final Vault vault;
	private final Stage window;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final SecureRandom csprng;
	private final ExecutorService executor;

	@Inject
	public CheckController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng, ExecutorService executor) {
		this.vault = vault;
		this.window = window;
		this.masterkey = Objects.requireNonNull(masterkeyRef.get());
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.csprng = csprng;
		this.executor = executor;
	}

	@FXML
	public void runCheck() {
		try (var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkey)) {
			HealthCheck.allChecks().stream()
					.peek(check -> {
						LOG.info("Running check: {}", check.identifier());
					})
					.flatMap(check -> check.check(vault.getPath(), vaultConfig, masterkey, cryptor, executor))
					.forEach(result -> {
						LOG.info("Result: {}", result);
					});
		}
	}

	public VaultConfig getVaultConfig() {
		return vaultConfig;
	}
}
