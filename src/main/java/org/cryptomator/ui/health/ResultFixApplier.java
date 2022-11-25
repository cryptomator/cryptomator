package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
class ResultFixApplier {

	private static final Logger LOG = LoggerFactory.getLogger(ResultFixApplier.class);

	private final Path vaultPath;
	private final SecureRandom csprng;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final ExecutorService sequentialExecutor;

	@Inject
	public ResultFixApplier(@HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vaultPath = vault.getPath();
		this.masterkey = masterkeyRef.get();
		this.vaultConfig = vaultConfigRef.get();
		this.csprng = csprng;
		this.sequentialExecutor = Executors.newSingleThreadExecutor();
	}

	public CompletionStage<Void> fix(Result result) {
		Preconditions.checkArgument(result.getState() == Result.FixState.FIXABLE);
		return CompletableFuture.runAsync(() -> result.setState(Result.FixState.FIXING), Platform::runLater) //
				.thenRunAsync(() -> fix(result.diagnosis()), sequentialExecutor) //
				.whenCompleteAsync((unused, throwable) -> {
					final Result.FixState s;
					if (throwable == null) {
						LOG.debug("Fix for {} applied successful.", result.diagnosis().getClass().getName());
						s = Result.FixState.FIXED;
					} else {
						LOG.error("Failed to apply fix for {}", result.diagnosis().getClass().getName(), throwable);
						s = Result.FixState.FIX_FAILED;
					}
					result.setState(s);
				}, Platform::runLater);
	}

	private void fix(DiagnosticResult diagnosis) {
		try (var masterkeyClone = masterkey.copy(); //
			 var cryptor = CryptorProvider.forScheme(vaultConfig.getCipherCombo()).provide(masterkeyClone, csprng)) {
			diagnosis.getFix(vaultPath, vaultConfig, masterkeyClone, cryptor) //
					.orElseThrow(() -> new IllegalStateException("No fix for diagnosis " + diagnosis.getClass().getName() + " implemented.")) //
					.apply();
		} catch (Exception e) {
			throw new FixFailedException(e);
		}
	}

	public static class FixFailedException extends CompletionException {

		private FixFailedException(Throwable cause) {
			super(cause);
		}
	}
}
