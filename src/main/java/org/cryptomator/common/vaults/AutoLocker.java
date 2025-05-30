package org.cryptomator.common.vaults;

import org.cryptomator.integrations.mount.UnmountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class AutoLocker {

	private static final Logger LOG = LoggerFactory.getLogger(AutoLocker.class);

	private final ScheduledExecutorService scheduler;
	private final ObservableList<Vault> vaultList;

	@Inject
	public AutoLocker(ScheduledExecutorService scheduler, ObservableList<Vault> vaultList) {
		this.scheduler = scheduler;
		this.vaultList = vaultList;
	}

	public void init() {
		scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.MINUTES);
	}

	private void tick() {
		vaultList.stream() // all vaults
				.filter(Vault::isUnlocked) // unlocked vaults
				.filter(this::exceedsIdleTime) // idle vaults
				.forEach(this::autolock);
	}

	private void autolock(Vault vault) {
		try {
			vault.lock(false);
			Platform.runLater(() -> vault.stateProperty().set(VaultState.Value.LOCKED));
			LOG.info("Autolocked {} after idle timeout", vault.getDisplayName());
		} catch (UnmountFailedException | IOException e) {
			LOG.error("Autolocking failed.", e);
		}
	}

	private boolean exceedsIdleTime(Vault vault) {
		assert vault.isUnlocked();
		if (vault.getVaultSettings().autoLockWhenIdle.get()) {
			int maxIdleSeconds = vault.getVaultSettings().autoLockIdleSeconds.get();
			var deadline = vault.getStats().getLastActivity().plusSeconds(maxIdleSeconds);
			return deadline.isBefore(Instant.now());
		} else {
			return false;
		}
	}


}
