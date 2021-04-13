package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Masterkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

class HealthCheckTask extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(HealthCheckTask.class);

	private final Path vaultPath;
	private final VaultConfig vaultConfig;
	private final Masterkey masterkey;
	private final SecureRandom csprng;
	private final HealthCheck check;
	private final Consumer<DiagnosticResult> resultConsumer;

	public HealthCheckTask(Path vaultPath, VaultConfig vaultConfig, Masterkey masterkey, SecureRandom csprng, HealthCheck check, Consumer<DiagnosticResult> resultConsumer) {
		this.vaultPath = vaultPath;
		this.vaultConfig = vaultConfig;
		this.masterkey = masterkey;
		this.csprng = csprng;
		this.check = Objects.requireNonNull(check);
		this.resultConsumer = resultConsumer;
	}

	@Override
	protected Void call() {
		try (var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkey)) {
			check.check(vaultPath, vaultConfig, masterkey, cryptor, result -> {
				if (isCancelled()) {
					throw new CancellationException();
				}
				Platform.runLater(() -> resultConsumer.accept(result));
			});
		}
		return null;
	}

	@Override
	protected void scheduled() {
		LOG.info("starting {}", check.identifier());
	}

	@Override
	protected void done() {
		LOG.info("finished {}", check.identifier());
	}
}
