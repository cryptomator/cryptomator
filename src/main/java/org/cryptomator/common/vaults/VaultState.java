package org.cryptomator.common.vaults;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValueBase;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@PerVault
public class VaultState extends ObservableValueBase<VaultState.Value> implements ObservableObjectValue<VaultState.Value> {

	private static final Logger LOG = LoggerFactory.getLogger(VaultState.class);

	public enum Value {
		/**
		 * No vault found at the provided path
		 */
		MISSING,

		/**
		 * No vault config found at the provided path
		 */
		VAULT_CONFIG_MISSING,

		/**
		 * No vault config and masterkey found at the provided path
		 */
		ALL_MISSING,

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
		ERROR
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

	/**
	 * Waits, without a timeout, until the state differs from the specified one.
	 *
	 * @param state what state to wait out
	 * @throws InterruptedException if the current thread is interrupted
	 */
	public void awaitStateOtherThan(Value state) throws InterruptedException {
		lock.lock();
		try {
			while (value.get() == state) {
				valueChanged.await();
			}
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
