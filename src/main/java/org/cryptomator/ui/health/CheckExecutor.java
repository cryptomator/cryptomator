package org.cryptomator.ui.health;

import com.google.common.collect.Comparators;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class CheckExecutor {

	private final Path vaultPath;
	private final SecureRandom csprng;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final ExecutorService sequentialExecutor;
	private final BlockingDeque<CheckTask> tasksToExecute;


	@Inject
	public CheckExecutor(@HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vaultPath = vault.getPath();
		this.masterkey = masterkeyRef.get();
		this.vaultConfig = vaultConfigRef.get();
		this.csprng = csprng;
		this.tasksToExecute = new LinkedBlockingDeque<>();
		this.sequentialExecutor = Executors.newSingleThreadExecutor();
	}

	public synchronized void executeBatch(List<Check> checks) {
		checks.stream().map(c -> {
			c.setState(Check.CheckState.SCHEDULED);
			var task = new CheckTask(c);
			tasksToExecute.addLast(task);
			return task;
		}).forEach(sequentialExecutor::submit);
	}

	public synchronized void cancel() {
		CheckTask task;
		while ((task = tasksToExecute.pollLast()) != null) {
			task.cancel(true);
		}
	}

	private class CheckTask extends Task<Void> {

		private final Check c;
		private DiagnosticResult.Severity highestResultSeverity = DiagnosticResult.Severity.GOOD;

		CheckTask(Check c) {
			this.c = c;
		}

		@Override
		protected Void call() throws Exception {
			try (var masterkeyClone = masterkey.copy(); //
				 var cryptor = CryptorProvider.forScheme(vaultConfig.getCipherCombo()).provide(masterkeyClone, csprng)) {
				c.getHealthCheck().check(vaultPath, vaultConfig, masterkeyClone, cryptor, diagnosis -> {
					Platform.runLater(() -> c.getResults().add(Result.create(diagnosis, vaultPath, vaultConfig, masterkeyClone, cryptor)));
					highestResultSeverity = Comparators.max(highestResultSeverity, diagnosis.getSeverity());
				});
			}
			return null;
		}

		@Override
		protected void running() {
			c.setState(Check.CheckState.RUNNING);
		}

		@Override
		protected void cancelled() {
			c.setState(Check.CheckState.CANCELLED);
		}

		@Override
		protected void succeeded() {
			c.setState(Check.CheckState.SUCCEEDED);
			c.setHighestResultSeverity(highestResultSeverity);
		}

		@Override
		protected void failed() {
			c.setState(Check.CheckState.ERROR);
			c.setError(this.getException());
		}

		@Override
		protected void done() {
			tasksToExecute.remove(this);
		}

	}

}