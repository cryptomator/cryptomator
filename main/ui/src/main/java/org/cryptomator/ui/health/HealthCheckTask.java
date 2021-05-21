package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Masterkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.CancellationException;

class HealthCheckTask extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(HealthCheckTask.class);

	private final Path vaultPath;
	private final VaultConfig vaultConfig;
	private final Masterkey masterkey;
	private final SecureRandom csprng;
	private final HealthCheck check;
	private final ObservableList<DiagnosticResultAction> results;

	public HealthCheckTask(Path vaultPath, VaultConfig vaultConfig, Masterkey masterkey, SecureRandom csprng, HealthCheck check) {
		this.vaultPath = Objects.requireNonNull(vaultPath);
		this.vaultConfig = Objects.requireNonNull(vaultConfig);
		this.masterkey = Objects.requireNonNull(masterkey);
		this.csprng = Objects.requireNonNull(csprng);
		this.check = Objects.requireNonNull(check);
		this.results = FXCollections.observableArrayList();

		var tmp = check.identifier();
		updateTitle(tmp.substring(tmp.length() - 10)); //TODO: new method with reliable logic
	}

	@Override
	protected Void call() {
		try (var masterkeyClone = masterkey.clone(); //
			 var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkeyClone)) {
			check.check(vaultPath, vaultConfig, masterkeyClone, cryptor, result -> {
				if (isCancelled()) {
					throw new CancellationException();
				}
				// FIXME: slowdown for demonstration purposes only:
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					if (isCancelled()) {
						return;
					} else {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
				}
				Platform.runLater(() -> results.add(new DiagnosticResultAction(result,vaultPath,vaultConfig, masterkey,csprng))); //FIXME: there can be a lotta results, each with a reference to the master key -> differentiate with severity!
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

	/* Getter */

	public ObservableList<DiagnosticResultAction> results() {
		return results;
	}

	public HealthCheck getCheck() {
		return check;
	}
}
