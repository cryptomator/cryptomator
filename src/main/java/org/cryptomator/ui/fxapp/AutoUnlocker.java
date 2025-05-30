package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

@FxApplicationScoped
public class AutoUnlocker {

	private static final Logger LOG = LoggerFactory.getLogger(AutoUnlocker.class);

	private final ObservableList<Vault> vaults;
	private final FxApplicationWindows appWindows;
	private final ScheduledExecutorService scheduler;
	private ScheduledFuture<?> unlockMissingFuture;
	private ScheduledFuture<?> timeoutFuture;

	@Inject
	public AutoUnlocker(ObservableList<Vault> vaults, FxApplicationWindows appWindows, ScheduledExecutorService scheduler) {
		this.vaults = vaults;
		this.appWindows = appWindows;
		this.scheduler = scheduler;
	}

	public void tryUnlockForTimespan(int timespan, TimeUnit timeUnit) {
		// Unlock all available auto unlock vaults
		Predicate<Vault> shouldAutoUnlock = v -> v.getVaultSettings().unlockAfterStartup.get();
		unlockSequentially(vaults.stream().filter(shouldAutoUnlock)).thenRun(() -> startUnlockMissing(timespan, timeUnit));
	}

	private CompletionStage<Void> unlockSequentially(Stream<Vault> vaultStream) {
		// this is an attempt to run all the unlock workflows sequentially, i.e. start the next workflow only after completing/failing the previous workflow.
		return vaultStream.filter(Vault::isLocked).reduce(CompletableFuture.completedFuture(null),
				(prevUnlock, nextVault) -> prevUnlock.thenCompose(_ -> appWindows.startUnlockWorkflow(nextVault, null)),
				(_, nextUnlock) -> nextUnlock.exceptionally(_ -> null) // we don't care here about the exception, logged elsewhere
				);
	}

	private void startUnlockMissing(int timespan, TimeUnit timeUnit) {
		// Start a temporary service if there are missing auto unlock vaults
		if (getMissingAutoUnlockVaults().findAny().isPresent()) {
			LOG.info("Found MISSING vaults, starting periodic check");
			unlockMissingFuture = scheduler.scheduleWithFixedDelay(this::unlockMissing, 0, 1, TimeUnit.SECONDS);
			timeoutFuture = scheduler.schedule(this::timeout, timespan, timeUnit);
		}
	}

	private void unlockMissing() {
		List<Vault> missingAutoUnlockVaults = getMissingAutoUnlockVaults().toList();
		missingAutoUnlockVaults.forEach(VaultListManager::redetermineVaultState);
		unlockSequentially(missingAutoUnlockVaults.stream()).thenRun(this::stopUnlockMissing);
	}

	private void stopUnlockMissing() {
		// Stop checking if there are no more missing vaults
		if (getMissingAutoUnlockVaults().findAny().isEmpty()) {
			LOG.info("No more MISSING vaults, stopping periodic check");
			unlockMissingFuture.cancel(false);
			timeoutFuture.cancel(false);
		}
	}

	private void timeout() {
		LOG.info("MISSING vaults periodic check timed out");
		unlockMissingFuture.cancel(false);
	}

	private Stream<Vault> getMissingAutoUnlockVaults() {
		return vaults.stream()
				.filter(Vault::isMissing)
				.filter(v -> v.getVaultSettings().unlockAfterStartup.get());
	}
}
