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

	public void tryUnlockForTimespan(int timespan) {
		// Unlock all available auto unlock vaults
		unlock(vaults.stream().filter(v -> v.getVaultSettings().unlockAfterStartup().get()));

		// Start a temporary service if there are missing auto unlock vaults
		if (getMissingAutoUnlockVaults().findAny().isPresent()) {
			LOG.info("Found MISSING vaults, starting periodic check");
			unlockMissingFuture = scheduler.scheduleWithFixedDelay(this::unlockMissing, 0, 1, TimeUnit.SECONDS);
			timeoutFuture = scheduler.schedule(this::timeout, timespan, TimeUnit.MINUTES);
		}
	}

	private void unlock(Stream<Vault> vaultStream) {
		vaultStream.filter(Vault::isLocked)
				.<CompletionStage<Void>>reduce(CompletableFuture.completedFuture(null),
				(unlockFlow, v) -> unlockFlow.handle((voit, ex) -> appWindows.startUnlockWorkflow(v, null)).thenCompose(stage -> stage), // we don't care here about the exception, logged elsewhere
				(unlockChain1, unlockChain2) -> unlockChain1.handle((voit, ex) -> unlockChain2).thenCompose(stage -> stage));
	}

	private void unlockMissing() {
		List<Vault> missingAutoUnlockVaults = getMissingAutoUnlockVaults().toList();
		missingAutoUnlockVaults.forEach(VaultListManager::redetermineVaultState);
		unlock(missingAutoUnlockVaults.stream());

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
				.filter(v -> v.getVaultSettings().unlockAfterStartup().get());
	}
}
