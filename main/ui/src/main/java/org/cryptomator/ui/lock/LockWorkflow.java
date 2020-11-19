package org.cryptomator.ui.lock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.concurrent.Task;

public class LockWorkflow extends Task<Boolean> {

	private static final Logger LOG = LoggerFactory.getLogger(LockWorkflow.class);

	private final Vault vault;

	@Inject
	public LockWorkflow(@LockWindow Vault vault) {
		this.vault = vault;
	}

	@Override
	protected Boolean call() throws Exception {
		// change vault state to processing -- done by overriding scheduled method of Task
		if (attemptLock() || attemptForcedLock()) {
			handleSuccess();
			return true;
		} else {
			//canceled -- for error the overriden failed() method is responsible
			return false;
		}
	}

	private boolean attemptLock() {
		try {
			vault.lock(false);
			return true;
		} catch (Volume.VolumeException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean attemptForcedLock() {
		// show forcedLock dialogue
		// wait for answer
		// depening on answer do one of two things
		// a) force Lock -> needs to throw exception on failure
		// b) cancel
		// if lock was performed over main window, show it again
		return false;
	}

	private void handleSuccess() {
		LOG.info("Lock of  {} succeeded.", vault.getDisplayName());
		// set vault state to locked
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
		LOG.info("Failed to lock {}.", vault.getDisplayName());
		vault.setState(VaultState.UNLOCKED);
	}

	@Override
	protected void cancelled() {
		vault.setState(VaultState.UNLOCKED);
	}

}
