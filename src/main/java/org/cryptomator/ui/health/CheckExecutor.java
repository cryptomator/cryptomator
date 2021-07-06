package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;

import javax.inject.Inject;
import javafx.application.Platform;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@HealthCheckScoped
public class CheckExecutor {

	private final Path vaultPath;
	private final SecureRandom csprng;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final ExecutorService sequentialExecutor;

	private volatile boolean isCanceled;


	@Inject
	public CheckExecutor(@HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vaultPath = vault.getPath();
		this.masterkey = masterkeyRef.get();
		this.vaultConfig = vaultConfigRef.get();
		this.csprng = csprng;
		this.sequentialExecutor = Executors.newSingleThreadExecutor();
	}

	public synchronized CompletionStage<Void> executeBatch(List<Check> checks) {
		isCanceled = false;
		var scheduledChecks = checks.stream().map(this::execute).toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(scheduledChecks);
	}

	//@formatter:off
	private CompletionStage<Void> execute(Check check) {
		Preconditions.checkArgument(check.isInReRunState());
		return CompletableFuture.runAsync(() -> check.setState(Check.CheckState.SCHEDULED), Platform::runLater)
			.thenApplyAsync(ignored -> {
					if (isCanceled) {
						throw new CancellationException();
					}
					Platform.runLater(() -> check.setState(Check.CheckState.RUNNING)); //must be set within the lambda
					var seenSeverities = EnumSet.noneOf(DiagnosticResult.Severity.class); //used due to efficiency and compactness
					check(check.getHealthCheck(), diagnosis -> {
						seenSeverities.add(diagnosis.getSeverity());
						Platform.runLater(() -> check.getResults().add(Result.create(diagnosis))); //observableLists need to be changed on FXThread
						if (isCanceled) {
							throw new CancellationException(); //hacky workaround to stop the check. DO NOT catch this exception (might be wrapped!)
						}
					});
					return determineHighesSeverity(seenSeverities); },
				sequentialExecutor)
			.handleAsync((maxSeenSeverity, throwable) -> {
					var endState = determineEndState(maxSeenSeverity,throwable);
					check.setState(endState);
					if( endState != Check.CheckState.CANCELLED) { //canceling throws exception
						check.setError(throwable);
					}
					return null; },
				Platform::runLater);
	}
	//@formatter:on

	private DiagnosticResult.Severity determineHighesSeverity(Set<DiagnosticResult.Severity> seenSeverities) {
		if (seenSeverities.contains(DiagnosticResult.Severity.CRITICAL)) {
			return DiagnosticResult.Severity.CRITICAL;
		} else if (seenSeverities.contains(DiagnosticResult.Severity.WARN)) {
			return DiagnosticResult.Severity.WARN;
		} else {
			return DiagnosticResult.Severity.GOOD;
		}
	}

	private Check.CheckState determineEndState(DiagnosticResult.Severity severity, Throwable t) {
		if (isCanceled) {
			//we do not check any exception, because CancellationExc might be wrapped
			return Check.CheckState.CANCELLED;
		} else if (t != null) {
			return Check.CheckState.ERROR;
		} else if (severity == DiagnosticResult.Severity.GOOD) {
			return Check.CheckState.ALL_GOOD;
		} else if (severity == DiagnosticResult.Severity.WARN) {
			return Check.CheckState.WITH_WARNINGS;
		} else {
			return Check.CheckState.WITH_CRITICALS;
		}
	}

	private void check(HealthCheck healthCheck, Consumer<DiagnosticResult> diagnosisConsumer) {
		try (var masterkeyClone = masterkey.clone(); //
			 var cryptor = CryptorProvider.forScheme(vaultConfig.getCipherCombo()).provide(masterkeyClone, csprng)) {
			healthCheck.check(vaultPath, vaultConfig, masterkeyClone, cryptor, diagnosisConsumer);
		} catch (Exception e) {
			throw new CheckFailedException(e);
		}
	}

	public synchronized void cancel() {
		isCanceled = true;
	}

	public static class CheckFailedException extends CompletionException {

		private CheckFailedException(Throwable cause) {
			super(cause);
		}
	}

}