package org.cryptomator.ui.common;

import com.google.common.collect.ImmutableList;
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
		Task<Vault> task = new LockVaultTask(vault, forced);
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vault.getDisplayableName()));
		task.setOnFailed(evt -> LOG.error("Failed to lock ", evt.getSource().getException()));
		executorService.execute(task);
	}

	public void lockAll(Collection<Vault> vaults, boolean forced) {
		Service<Vault> service = createLockAllService(vaults, forced);
		service.setOnSucceeded(evt -> LOG.info("Locked {}", service.getValue().getDisplayableName()));
		service.setOnFailed(evt -> LOG.error("Failed to lock vault", evt.getSource().getException()));
		service.start();
	}

	/**
	 * Creates but doesn't start a lock-all service that can be run on a background thread.
	 *
	 * @param vaults The list of vaults to be locked
	 * @param forced Whether to attempt a forced lock
	 * @return Service that tries to lock all given vaults and cancels itself automatically when done
	 */
	public Service<Vault> createLockAllService(Collection<Vault> vaults, boolean forced) {
		Iterator<Vault> iter = ImmutableList.copyOf(vaults).iterator();
		ScheduledService<Vault> service = new ScheduledService<>() {

			@Override
			protected Task<Vault> createTask() {
				assert Platform.isFxApplicationThread();
				if (iter.hasNext()) {
					return new LockVaultTask(iter.next(), forced);
				} else {
					cancel();
					return new IllegalStateTask("This task should never be executed.");
				}
			}
		};
		service.setExecutor(executorService);
		return service;
	}

	/**
	 * A task that locks a vault
	 */
	private static class LockVaultTask extends Task<Vault> {

		private final Vault vault;
		private final boolean forced;

		/**
		 * @param vault The vault to lock
		 * @param forced Whether to attempt a forced lock
		 */
		public LockVaultTask(Vault vault, boolean forced) {
			this.vault = vault;
			this.forced = forced;
		}

		@Override
		protected Vault call() throws Volume.VolumeException {
			vault.lock(forced);
			return vault;
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

	}

	/**
	 * A task that throws an IllegalStateException
	 */
	private static class IllegalStateTask<V> extends Task<V> {

		private final String message;

		/**
		 * @param message The message of the IllegalStateException
		 */
		public IllegalStateTask(String message) {
			this.message = message;
		}

		@Override
		protected V call() throws IllegalStateException {
			throw new IllegalStateException(message);
		}
	}

}
