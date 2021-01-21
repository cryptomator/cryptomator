package org.cryptomator.ui.common;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.concurrent.Task;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@FxApplicationScoped
public class VaultService {

	private static final Logger LOG = LoggerFactory.getLogger(VaultService.class);

	private final ExecutorService executorService;
	private final HostServiceRevealer vaultRevealer;

	@Inject
	public VaultService(ExecutorService executorService, HostServiceRevealer vaultRevealer) {
		this.executorService = executorService;
		this.vaultRevealer = vaultRevealer;
	}

	public void reveal(Vault vault) {
		executorService.execute(createRevealTask(vault));
	}

	/**
	 * Creates but doesn't start a reveal task.
	 *
	 * @param vault The vault to reveal
	 */
	public Task<Vault> createRevealTask(Vault vault) {
		Task<Vault> task = new RevealVaultTask(vault, vaultRevealer);
		task.setOnSucceeded(evt -> LOG.info("Revealed {}", vault.getDisplayName()));
		task.setOnFailed(evt -> LOG.error("Failed to reveal " + vault.getDisplayName(), evt.getSource().getException()));
		return task;
	}

	/**
	 * Locks a vault in a background thread.
	 *
	 * @param vault The vault to lock
	 * @param forced Whether to attempt a forced lock
	 */
	public void lock(Vault vault, boolean forced) {
		executorService.execute(createLockTask(vault, forced));
	}

	/**
	 * Creates but doesn't start a lock task.
	 *
	 * @param vault The vault to lock
	 * @param forced Whether to attempt a forced lock
	 * @return The task
	 */
	public Task<Vault> createLockTask(Vault vault, boolean forced) {
		Task<Vault> task = new LockVaultTask(vault, forced);
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vault.getDisplayName()));
		task.setOnFailed(evt -> LOG.info("Failed to lock {}.", vault.getDisplayName(), evt.getSource().getException()));
		return task;
	}

	/**
	 * Locks all given vaults in a background thread.
	 *
	 * @param vaults The vaults to lock
	 * @param forced Whether to attempt a forced lock
	 */
	public void lockAll(Collection<Vault> vaults, boolean forced) {
		executorService.execute(createLockAllTask(vaults, forced));
	}

	/**
	 * Creates but doesn't start a lock-all task.
	 *
	 * @param vaults The list of vaults to be locked
	 * @param forced Whether to attempt a forced lock
	 * @return Meta-Task that waits until all vaults are locked or fails after the first failure of a subtask
	 */
	public Task<Collection<Vault>> createLockAllTask(Collection<Vault> vaults, boolean forced) {
		List<Task<Vault>> lockTasks = vaults.stream().map(v -> new LockVaultTask(v, forced)).collect(Collectors.toUnmodifiableList());
		lockTasks.forEach(executorService::execute);
		Task<Collection<Vault>> task = new WaitForTasksTask(lockTasks);
		String vaultNames = vaults.stream().map(Vault::getDisplayName).collect(Collectors.joining(", "));
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vaultNames));
		task.setOnFailed(evt -> LOG.error("Failed to lock vaults " + vaultNames, evt.getSource().getException()));
		return task;
	}

	private static class RevealVaultTask extends Task<Vault> {

		private final Vault vault;
		private final Volume.Revealer revealer;

		/**
		 * @param vault The vault to lock
		 * @param revealer The object to use to show the vault content to the user.
		 */
		public RevealVaultTask(Vault vault, Volume.Revealer revealer) {
			this.vault = vault;
			this.revealer = revealer;

			setOnFailed(evt -> LOG.error("Failed to reveal " + vault.getDisplayName(), getException()));
		}

		@Override
		protected Vault call() throws Volume.VolumeException {
			vault.reveal(revealer);
			return vault;
		}
	}

	/**
	 * A task that waits for completion of multiple other tasks
	 */
	private static class WaitForTasksTask extends Task<Collection<Vault>> {

		private final Collection<Task<Vault>> startedTasks;

		public WaitForTasksTask(Collection<Task<Vault>> tasks) {
			this.startedTasks = List.copyOf(tasks);

			setOnFailed(event -> LOG.error("Failed to lock multiple vaults", getException()));
		}

		@Override
		protected Collection<Vault> call() throws ExecutionException, InterruptedException {
			Iterator<Task<Vault>> remainingTasks = startedTasks.iterator();
			Collection<Vault> completed = new ArrayList<>();
			try {
				// wait for all tasks:
				while (remainingTasks.hasNext()) {
					Vault done = remainingTasks.next().get();
					completed.add(done);
				}
			} catch (ExecutionException e) {
				// cancel all remaining:
				while (remainingTasks.hasNext()) {
					remainingTasks.next().cancel(true);
				}
				throw e;
			}
			return completed;
		}
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

			setOnFailed(event -> LOG.error("Failed to lock " + vault.getDisplayName(), event.getSource().getException()));
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


}
