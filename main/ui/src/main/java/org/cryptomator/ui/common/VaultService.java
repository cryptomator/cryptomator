package org.cryptomator.ui.common;

import javafx.concurrent.Task;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@FxApplicationScoped
public class VaultService {

	private static final Logger LOG = LoggerFactory.getLogger(VaultService.class);

	private final ExecutorService executorService;
	private final Optional<KeychainAccess> keychain;

	@Inject
	public VaultService(ExecutorService executorService, Optional<KeychainAccess> keychain) {
		this.executorService = executorService;
		this.keychain = keychain;
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
		Task<Vault> task = new RevealVaultTask(vault);
		task.setOnSucceeded(evt -> LOG.info("Revealed {}", vault.getDisplayableName()));
		task.setOnFailed(evt -> LOG.error("Failed to reveal " + vault.getDisplayableName(), evt.getSource().getException()));
		return task;
	}

	/**
	 * Attempts to unlock all given vaults in a background thread using passwords stored in the system keychain.
	 *
	 * @param vaults The vaults to unlock
	 * @implNote No-op if no system keychain is present
	 */
	public void attemptAutoUnlock(Collection<Vault> vaults) {
		if (!keychain.isPresent()) {
			LOG.debug("No system keychain found. Unable to auto unlock without saved passwords.");
		} else {
			List<Task<Vault>> unlockTasks = vaults.stream().map(v -> createAutoUnlockTask(v, keychain.get())).collect(Collectors.toList());
			Task<Collection<Vault>> runSequentiallyTask = new RunSequentiallyTask(unlockTasks);
			executorService.execute(runSequentiallyTask);
		}
	}

	/**
	 * Creates but doesn't start an auto-unlock task.
	 *
	 * @param vault The vault to unlock
	 * @param keychainAccess The system keychain holding the passphrase for the vault
	 * @return The task
	 */
	public Task<Vault> createAutoUnlockTask(Vault vault, KeychainAccess keychainAccess) {
		Task<Vault> task = new AutoUnlockVaultTask(vault, keychainAccess);
		task.setOnSucceeded(evt -> LOG.info("Auto-unlocked {}", vault.getDisplayableName()));
		task.setOnFailed(evt -> LOG.error("Failed to auto-unlock " + vault.getDisplayableName(), evt.getSource().getException()));
		return task;
	}

	/**
	 * Unlocks a vault in a background thread
	 *
	 * @param vault The vault to unlock
	 * @param passphrase The password to use - wipe this param asap
	 * @implNote A copy of the passphrase will be made, which is wiped as soon as the task ran.
	 */
	public void unlock(Vault vault, CharSequence passphrase) {
		executorService.execute(createUnlockTask(vault, passphrase));
	}

	/**
	 * Creates but doesn't start an unlock task.
	 *
	 * @param vault The vault to unlock
	 * @param passphrase The password to use - wipe this param asap
	 * @return The task
	 * @implNote A copy of the passphrase will be made, which is wiped as soon as the task ran.
	 */
	public Task<Vault> createUnlockTask(Vault vault, CharSequence passphrase) {
		Task<Vault> task = new UnlockVaultTask(vault, passphrase);
		task.setOnSucceeded(evt -> LOG.info("Unlocked {}", vault.getDisplayableName()));
		task.setOnFailed(evt -> LOG.error("Failed to unlock " + vault.getDisplayableName(), evt.getSource().getException()));
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
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vault.getDisplayableName()));
		task.setOnFailed(evt -> LOG.error("Failed to lock " + vault.getDisplayableName(), evt.getSource().getException()));
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
		String vaultNames = vaults.stream().map(Vault::getDisplayableName).collect(Collectors.joining(", "));
		task.setOnSucceeded(evt -> LOG.info("Locked {}", vaultNames));
		task.setOnFailed(evt -> LOG.error("Failed to lock vaults " + vaultNames, evt.getSource().getException()));
		return task;
	}

	private static class RevealVaultTask extends Task<Vault> {

		private final Vault vault;

		/**
		 * @param vault The vault to lock
		 */
		public RevealVaultTask(Vault vault) {
			this.vault = vault;
		}

		@Override
		protected Vault call() throws Volume.VolumeException {
			vault.reveal();
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
	 * A task that runs a list of tasks in their given order
	 */
	private static class RunSequentiallyTask extends Task<Collection<Vault>> {

		private final List<Task<Vault>> tasks;

		public RunSequentiallyTask(List<Task<Vault>> tasks) {
			this.tasks = List.copyOf(tasks);
		}

		@Override
		protected List<Vault> call() throws ExecutionException, InterruptedException {
			List<Vault> completed = new ArrayList<>();
			for (Task<Vault> task : tasks) {
				task.run();
				Vault done = task.get();
				completed.add(done);
			}
			return completed;
		}
	}

	private static class AutoUnlockVaultTask extends Task<Vault> {

		private final Vault vault;
		private final KeychainAccess keychain;

		public AutoUnlockVaultTask(Vault vault, KeychainAccess keychain) {
			this.vault = vault;
			this.keychain = keychain;
		}

		@Override
		protected Vault call() throws Exception {
			char[] storedPw = null;
			try {
				storedPw = keychain.loadPassphrase(vault.getId());
				if (storedPw == null) {
					throw new InvalidPassphraseException();
				}
				vault.unlock(CharBuffer.wrap(storedPw));
			} finally {
				if (storedPw != null) {
					Arrays.fill(storedPw, ' ');
				}
			}
			return vault;
		}

		@Override
		protected void scheduled() {
			vault.setState(VaultState.PROCESSING);
		}

		@Override
		protected void succeeded() {
			vault.setState(VaultState.UNLOCKED);
		}

		@Override
		protected void failed() {
			vault.setState(VaultState.LOCKED);
		}
	}

	private static class UnlockVaultTask extends Task<Vault> {

		private final Vault vault;
		private final CharBuffer passphrase;

		/**
		 * @param vault The vault to unlock
		 * @param passphrase The password to use - wipe this param asap
		 * @implNote A copy of the passphrase will be made, which is wiped as soon as the task ran.
		 */
		public UnlockVaultTask(Vault vault, CharSequence passphrase) {
			this.vault = vault;
			this.passphrase = CharBuffer.allocate(passphrase.length());
			for (int i = 0; i < passphrase.length(); i++) {
				this.passphrase.put(i, passphrase.charAt(i));
			}
		}

		@Override
		protected Vault call() throws Exception {
			try {
				vault.unlock(passphrase);
			} finally {
				Arrays.fill(passphrase.array(), ' ');
			}
			return vault;
		}

		@Override
		protected void scheduled() {
			vault.setState(VaultState.PROCESSING);
		}

		@Override
		protected void succeeded() {
			vault.setState(VaultState.UNLOCKED);
		}

		@Override
		protected void failed() {
			vault.setState(VaultState.LOCKED);
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
