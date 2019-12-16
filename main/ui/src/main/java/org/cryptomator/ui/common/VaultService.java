package org.cryptomator.ui.common;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

@FxApplicationScoped
public class VaultService {

	private static final Logger LOG = LoggerFactory.getLogger(VaultService.class);

	private final ExecutorService executorService;

	@Inject
	public VaultService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	/**
	 * Locks a vault in a background thread.
	 *
	 * @param vault The vault to lock
	 * @param forced Whether to attempt a forced lock
	 */
	public void lock(Vault vault, boolean forced) {
		Task<Void> task = createLockTask(vault, forced);
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vault.getDisplayableName()));
		task.setOnFailed(evt -> LOG.error("Failed to lock vault " + vault.getDisplayableName(), evt.getSource().getException()));
		executorService.execute(task);
	}

	public void lockAll(Collection<Vault> vaults, boolean forced) {
		Service<Void> service = createLockAllService(vaults, forced);
		service.setOnFailed(evt -> LOG.error("Failed to lock vault", evt.getSource().getException()));
		service.setExecutor(executorService);
		service.start();
	}

	/**
	 * Creates but doesn't start a lock-all service that can be run on a background thread.
	 *
	 * @param vaults The list of vaults to be locked. Must not be concurrently modified
	 * @param forced Whether to attempt a forced lock
	 * @return Service that tries to lock all given vaults
	 */
	public Service<Void> createLockAllService(Collection<Vault> vaults, boolean forced) {
		Iterator<Vault> iter = vaults.iterator();
		ScheduledService<Void> service = new ScheduledService<>() {

			@Override
			protected Task<Void> createTask() {
				assert Platform.isFxApplicationThread();
				if (iter.hasNext()) {
					return createLockTask(iter.next(), forced);
				} else {
					// This should be unreachable code, since iter is only accessed on the FX App Thread.
					// But if quitting the application takes longer for any reason, this service should shut down properly
					reset();
					return createNoopTask();
				}
			}
		};
		service.setExecutor(executorService);
		return service;
	}

	/**
	 * Creates but doesn't start a lock task that can be run on a background thread.
	 *
	 * @param vault The vault to lock
	 * @param forced Whether to attempt a forced lock
	 * @return Task that tries to lock the given vault
	 */
	public Task<Void> createLockTask(Vault vault, boolean forced) {
		return new Task<>() {
			@Override
			protected Void call() throws Volume.VolumeException {
				vault.lock(forced);
				return null;
			}

			@Override
			protected void scheduled() {
				vault.setState(VaultState.PROCESSING);
			}

			@Override
			protected void succeeded() {
				vault.setState(VaultState.LOCKED);
			}

			@Override
			protected void failed() {
				vault.setState(VaultState.UNLOCKED);
			}
		};
	}

	private Task<Void> createNoopTask() {
		return new Task<>() {
			@Override
			protected Void call() {
				return null;
			}
		};
	}

}
