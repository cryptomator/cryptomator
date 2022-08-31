package org.cryptomator.common.vaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.collections.ObservableList;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

@Singleton
public class AutoLocker {

	private static final Logger LOG = LoggerFactory.getLogger(AutoLocker.class);

	private final ExecutorService executor;
	private final Timer timer;
	private final ObservableList<Vault> vaultList;

	@Inject
	public AutoLocker(ExecutorService executor, ObservableList<Vault> vaultList) {
		this.executor = executor;
		this.timer = new Timer("AutoLocker.SubmitTickTimer", true);
		this.vaultList = vaultList;
	}

	public void init() {
		TimerTask submitTask = new TimerTask() {
			@Override
			public void run() {
				executor.submit(AutoLocker.this::tick);
			}
		};
		timer.scheduleAtFixedRate(submitTask, 0, 1000);
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
			LOG.info("Autolocked {} after idle timeout", vault.getDisplayName());
		} catch (Volume.VolumeException | LockNotCompletedException e) {
			LOG.error("Autolocking failed.", e);
		}
	}

	private boolean exceedsIdleTime(Vault vault) {
		assert vault.isUnlocked();
		if (vault.getVaultSettings().autoLockWhenIdle().get()) {
			int maxIdleSeconds = vault.getVaultSettings().autoLockIdleSeconds().get();
			var deadline = vault.getStats().getLastActivity().plusSeconds(maxIdleSeconds);
			return deadline.isBefore(Instant.now());
		} else {
			return false;
		}
	}


}
