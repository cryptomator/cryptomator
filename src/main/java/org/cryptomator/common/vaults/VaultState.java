package org.cryptomator.common.vaults;

import com.google.common.base.Preconditions;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.DirStructure;
import org.cryptomator.cryptofs.migration.Migrators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValueBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;

@PerVault
public class VaultState extends ObservableValueBase<VaultState.Value> implements ObservableObjectValue<VaultState.Value> {

	private static final Logger LOG = LoggerFactory.getLogger(VaultState.class);

	public enum Value {
		/**
		 * No vault found at the provided path
		 */
		MISSING,

		/**
		 * Vault requires migration to a newer vault format
		 */
		NEEDS_MIGRATION,

		/**
		 * Vault ready to be unlocked
		 */
		LOCKED,

		/**
		 * Vault in transition between two other states
		 */
		PROCESSING,

		/**
		 * Vault is unlocked
		 */
		UNLOCKED,

		/**
		 * Unknown state due to preceding unrecoverable exceptions.
		 */
		ERROR;
	}

	private final AtomicReference<Value> value;
	private final Lock lock = new ReentrantLock();
	private final Condition valueChanged = lock.newCondition();

	@Inject
	public VaultState(VaultState.Value initialValue) {
		this.value = new AtomicReference<>(initialValue);
	}

	@Override
	public Value get() {
		return getValue();
	}

	@Override
	public Value getValue() {
		return value.get();
	}

	/**
	 * Determines the appropriate vault state based on the vault's directory structure.
	 *
	 * @param pathToVault Path to the vault directory
	 * @return The determined vault state
	 * @throws IOException If an I/O error occurs while checking the vault
	 */
	public static Value determineForPath(Path pathToVault) throws IOException {
		if (!Files.exists(pathToVault)) {
			return Value.MISSING;
		}
		return switch (CryptoFileSystemProvider.checkDirStructureForVault(pathToVault, VAULTCONFIG_FILENAME, MASTERKEY_FILENAME)) {
			case VAULT -> Value.LOCKED;
			case UNRELATED -> Value.MISSING;
			case MAYBE_LEGACY -> Migrators.get().needsMigration(pathToVault, VAULTCONFIG_FILENAME, MASTERKEY_FILENAME) ?
					Value.NEEDS_MIGRATION :
					Value.MISSING;
		};
	}

	/**
	 * Redetermines the vault state based on current conditions.
	 *
	 * @param vaultPath Path to the vault directory
	 * @param configCache Vault configuration cache
	 * @return The determined vault state
	 */
	public Value redetermine(Path vaultPath, VaultConfigCache configCache) throws IOException {
		var determinedState = determineForPath(vaultPath);
		if (determinedState == Value.LOCKED) {
			configCache.reloadConfig();
		}
		return determinedState;
	}

	/**
	 * Transitions from <code>fromState</code> to <code>toState</code>.
	 *
	 * @param fromState Previous state
	 * @param toState New state
	 * @return <code>true</code> if successful
	 */
	public boolean transition(Value fromState, Value toState) {
		Preconditions.checkArgument(fromState != toState, "fromState must be different than toState");
		boolean success = value.compareAndSet(fromState, toState);
		if (success) {
			fireValueChangedEvent();
		} else {
			LOG.debug("Failed transiting into state {}: Expected state was not {}.", fromState, toState);
		}
		return success;
	}

	public void set(Value newState) {
		var oldState = value.getAndSet(newState);
		if (oldState != newState) {
			fireValueChangedEvent();
		}
	}

	/**
	 * Waits for the specified time, until the desired state is reached.
	 *
	 * @param desiredState what state to wait for
	 * @param time the maximum time to wait
	 * @param unit the time unit of the {@code time} argument
	 * @return {@code false} if the waiting time detectably elapsed before reaching {@code desiredState}
	 * @throws InterruptedException if the current thread is interrupted
	 */
	public boolean awaitState(Value desiredState, long time, TimeUnit unit) throws InterruptedException {
		lock.lock();
		try {
			long remaining = TimeUnit.NANOSECONDS.convert(time, unit);
			while (value.get() != desiredState) {
				if (remaining <= 0L) {
					return false;
				}
				remaining = valueChanged.awaitNanos(remaining);
			}
			return true;
		} finally {
			lock.unlock();
		}
	}

	private void signal() {
		lock.lock();
		try {
			valueChanged.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void fireValueChangedEvent() {
		signal();
		if (Platform.isFxApplicationThread()) {
			super.fireValueChangedEvent();
		} else {
			Platform.runLater(super::fireValueChangedEvent);
		}
	}
}
