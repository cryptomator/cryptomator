package org.cryptomator.ui.common;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.mount.Mountpoint;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@FxApplicationScoped
public class VaultService {

	private static final Logger LOG = LoggerFactory.getLogger(VaultService.class);

	private final RevealPathService revealPathService;
	private final ExecutorService executorService;

	@Inject
	public VaultService(RevealPathService revealPathService, ExecutorService executorService) {
		this.revealPathService = revealPathService;
		this.executorService = executorService;
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
		Task<Vault> task = new RevealVaultTask(vault, revealPathService);
		task.setOnSucceeded(_ -> LOG.info("Revealed {}", vault.getDisplayName()));
		task.setOnFailed(evt -> LOG.warn("Failed to reveal {}", vault.getDisplayName(), evt.getSource().getException()));
		return task;
	}

	/**
	 * Locks a vault in a background thread.
	 *
	 * @param vault The vault to lock
	 * @param forced Whether to attempt a forced lock
	 * @deprecated use {@link org.cryptomator.ui.fxapp.FxApplicationWindows#startLockWorkflow(Vault, Stage)}
	 */
	@Deprecated
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
	 * Creates a lock-all task.
	 * This task itself is _not started_, but its subtasks locking each vault will be already executed.
	 *
	 * @param vaults The list of vaults to be locked
	 * @param forced Whether to attempt a forced lock
	 * @return Meta-Task that waits for all subtasks, which skip vaults that are not unlocked, and fails afterwards if any subtask failed
	 */
	public Task<Collection<Vault>> createLockAllTask(Collection<Vault> vaults, boolean forced) {
		List<Task<Vault>> lockTasks = vaults.stream().<Task<Vault>>map(v -> new LockVaultTask(v, forced)).toList();
		lockTasks.forEach(executorService::execute);
		Task<Collection<Vault>> task = new WaitForTasksTask(lockTasks);
		String vaultNames = vaults.stream().map(Vault::getDisplayName).collect(Collectors.joining(", "));
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vaultNames));
		task.setOnFailed(evt -> LOG.error("Failed to lock vaults " + vaultNames, evt.getSource().getException()));
		return task;
	}

	private static class RevealVaultTask extends Task<Vault> {

		private final Vault vault;
		private final RevealPathService rs;

		public RevealVaultTask(Vault vault, RevealPathService revealPathService) {
			this.vault = vault;
			this.rs = revealPathService;
		}

		@Override
		protected Vault call() throws RevealFailedException {
			switch (vault.getMountPoint()) {
				case null -> LOG.warn("Not currently mounted");
				case Mountpoint.WithPath m -> rs.reveal(m.path());
				case Mountpoint.WithUri m -> LOG.info("Vault mounted at {}", m.uri()); // TODO show in UI?
			}
			return vault;
		}
	}

	/**
	 * A task that waits for completion of multiple other tasks and fails once all are done, if any failed
	 */
	private static class WaitForTasksTask extends Task<Collection<Vault>> {

		private final Collection<Task<Vault>> startedTasks;

		public WaitForTasksTask(Collection<Task<Vault>> tasks) {
			this.startedTasks = List.copyOf(tasks);

			setOnFailed(event -> LOG.error("Failed to lock multiple vaults", getException()));
		}

		@Override
		protected Collection<Vault> call() throws ExecutionException, InterruptedException {
			Collection<Vault> completed = new ArrayList<>();
			ExecutionException firstFailure = null;
			for (Task<Vault> task : startedTasks) {
				try {
					completed.add(task.get());
				} catch (ExecutionException e) {
					if (firstFailure == null) {
						firstFailure = e;
					}
				}
			}
			if (firstFailure != null) {
				throw firstFailure;
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
		protected Vault call() throws UnmountFailedException, IOException, InterruptedException {
			var state = vault.stateProperty();
			while (!state.transition(VaultState.Value.UNLOCKED, VaultState.Value.PROCESSING)) {
				switch (state.get()) {
					case UNLOCKED -> {} // settled by another workflow in the meantime, retry
					case PROCESSING -> state.awaitStateOtherThan(VaultState.Value.PROCESSING);
					default -> {
						LOG.debug("Skipping lock of {}, which is not unlocked.", vault.getDisplayName());
						return vault;
					}
				}
			}
			try {
				vault.lock(forced);
				return vault;
			} finally {
				vault.finishProcessing();
			}
		}

	}


}
