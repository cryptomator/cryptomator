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
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

class HealthCheckTask extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(HealthCheckTask.class);

	private final Path vaultPath;
	private final VaultConfig vaultConfig;
	private final Masterkey masterkey;
	private final SecureRandom csprng;
	private final HealthCheck check;
	private final ObservableList<DiagnosticResult> results;

	private final AtomicReference<State> endState;
	private final AtomicReference<Throwable> exceptionOnDone;

	public HealthCheckTask(Path vaultPath, VaultConfig vaultConfig, Masterkey masterkey, SecureRandom csprng, HealthCheck check) {
		this.vaultPath = Objects.requireNonNull(vaultPath);
		this.vaultConfig = Objects.requireNonNull(vaultConfig);
		this.masterkey = Objects.requireNonNull(masterkey);
		this.csprng = Objects.requireNonNull(csprng);
		this.check = Objects.requireNonNull(check);
		this.results = FXCollections.observableArrayList();
		this.endState = new AtomicReference<>(null);
		this.exceptionOnDone = new AtomicReference<>();

		var tmp = check.identifier();
		updateTitle(tmp.substring(tmp.length() - 10)); //TODO: new method with reliable logic
	}

	@Override
	protected Void call() {
		try (var masterkeyClone = masterkey.clone(); //
			 var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkeyClone)) {
			check.check(vaultPath, vaultConfig, masterkeyClone, cryptor, result -> { //TODO: API-question about using Masterkey _and_ cryptor
				if (isCancelled()) {
					throw new CancellationException();
				}
				// FIXME: slowdown for demonstration purposes only:
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					if(isCancelled()) {
						return;
					} else {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
				}
				Platform.runLater(() -> results.add(result));
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
		Platform.runLater(() -> endState.set(getState()));
	}

	@Override
	protected void failed() {
		Platform.runLater(() -> exceptionOnDone.set(getException()));
	}

	/* Getter */

	public ObservableList<DiagnosticResult> results() {
		return results;
	}

	public HealthCheck getCheck() {
		return check;
	}

	public State getEndState() {
		return endState.get();
	}

	public Optional<Throwable> getExceptionOnDone() {
		return Optional.ofNullable(exceptionOnDone.get());
	}
}
