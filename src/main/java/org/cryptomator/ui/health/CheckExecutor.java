package org.cryptomator.ui.health;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;

import javax.inject.Inject;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class CheckExecutor {

	private final Path vaultPath;
	private final SecureRandom csprng;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final ExecutorService sequentialExecutor;
	private final Queue<CheckTask> runningTasks;


	@Inject
	public CheckExecutor(@HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vaultPath = vault.getPath();
		this.masterkey = masterkeyRef.get();
		this.vaultConfig = vaultConfigRef.get();
		this.csprng = csprng;
		this.sequentialExecutor = Executors.newSingleThreadExecutor();
		this.runningTasks = new ArrayDeque<>();
	}

	public synchronized void executeBatch(List<Check> checks) {
		checks.stream().map(c -> {
			c.setState(Check.CheckState.SCHEDULED);
			var task = new CheckTask(c);
			runningTasks.add(task); // we need to use CheckTask and not Futures to set state to CANCEL
			return task;
		}).forEach(sequentialExecutor::submit);
	}

	public synchronized void cancel() {
		while (!runningTasks.isEmpty()) {
			runningTasks.remove().cancel(true);
		}
	}

	private class CheckTask extends Task<Void> {

		private Check c;
		private DiagnosticResult.Severity highestResultSeverity;

		CheckTask(Check c) {
			this.c = c;
		}

		@Override
		protected Void call() throws Exception {
			try (var masterkeyClone = masterkey.clone(); //
				 var cryptor = CryptorProvider.forScheme(vaultConfig.getCipherCombo()).provide(masterkeyClone, csprng)) {
				c.getHealthCheck().check(vaultPath, vaultConfig, masterkeyClone, cryptor, diagnosis -> {
					c.getResults().add(Result.create(diagnosis));
					compareAndSetSeverity(diagnosis.getSeverity());
				});
			}
			return null;
		}

		private void compareAndSetSeverity(DiagnosticResult.Severity newOne) {
			if (highestResultSeverity != DiagnosticResult.Severity.CRITICAL && newOne == DiagnosticResult.Severity.CRITICAL) {
				highestResultSeverity = DiagnosticResult.Severity.CRITICAL;
			} else if (highestResultSeverity != DiagnosticResult.Severity.WARN && newOne == DiagnosticResult.Severity.WARN) {
				highestResultSeverity = DiagnosticResult.Severity.WARN;
			} else if (highestResultSeverity != DiagnosticResult.Severity.GOOD && newOne == DiagnosticResult.Severity.GOOD) {
				highestResultSeverity = DiagnosticResult.Severity.GOOD;
			} else {
				highestResultSeverity = DiagnosticResult.Severity.INFO;
			}
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

	}

}