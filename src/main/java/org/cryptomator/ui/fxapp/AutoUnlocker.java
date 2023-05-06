package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.collections.ObservableList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@FxApplicationScoped
public class AutoUnlocker {

	private static final Logger LOG = LoggerFactory.getLogger(AutoUnlocker.class);

	private final ObservableList<Vault> vaults;
	private final FxApplicationWindows appWindows;
	private final ScheduledExecutorService scheduler;
	private ScheduledFuture<?> checkFuture;
	private ScheduledFuture<?> timeoutFuture;
	private boolean isPeriodicCheckActive = false;

	@Inject
	public AutoUnlocker(ObservableList<Vault> vaults, FxApplicationWindows appWindows, ScheduledExecutorService scheduler) {
		this.vaults = vaults;
		this.appWindows = appWindows;
		this.scheduler = scheduler;
	}

	public void unlockAll() {
		unlock(vaults.stream().filter(v -> v.getVaultSettings().unlockAfterStartup().get()));
	}

	public void unlock(Stream<Vault> vaultStream) {
		vaultStream.filter(Vault::isLocked)
				.<CompletionStage<Void>>reduce(CompletableFuture.completedFuture(null),
				(unlockFlow, v) -> unlockFlow.handle((voit, ex) -> appWindows.startUnlockWorkflow(v, null)).thenCompose(stage -> stage), // we don't care here about the exception, logged elsewhere
				(unlockChain1, unlockChain2) -> unlockChain1.handle((voit, ex) -> unlockChain2).thenCompose(stage -> stage));
	}

	public void startMissingVaultsChecker() {
		if (!isPeriodicCheckActive && getMissingAutoUnlockVaults().count() > 0) {
			LOG.info("Found MISSING vaults, starting periodic check");
			checkFuture = scheduler.scheduleWithFixedDelay(this::check, 0, 1, TimeUnit.SECONDS);
			timeoutFuture = scheduler.schedule(this::timeout, 2, TimeUnit.MINUTES);
			isPeriodicCheckActive = true;
		}
	}

	private void check() {
		// Find the vaults that are missing but have an existing directory
		Vault[] vaultArray = getMissingAutoUnlockVaults().filter(v -> v.getPath().toFile().isDirectory()).toArray(Vault[]::new);
		if (vaultArray.length > 0) {

			// Redetermine vault states
			for (Vault v : vaultArray) {
				LOG.info("Found vault directory for '{}'", v.getDisplayName());
				VaultListManager.redetermineVaultState(v);
			}

			// Unlock the vaults that were previously missing
			unlock(Arrays.stream(vaultArray));
		}

		// Stop checking if there are no more missing vaults
		if (getMissingAutoUnlockVaults().count() == 0) {
			LOG.info("No more MISSING vaults, stopping periodic check");
			isPeriodicCheckActive = false;
			checkFuture.cancel(false);
			timeoutFuture.cancel(false);
		}
	}

	private void timeout() {
		LOG.info("MISSING vaults periodic check timed out");
		isPeriodicCheckActive = false;
		checkFuture.cancel(false);
	}

	private Stream<Vault> getMissingAutoUnlockVaults() {
		return vaults.stream()
				.filter(Vault::isMissing)
				.filter(v -> v.getVaultSettings().unlockAfterStartup().get());
	}
}
