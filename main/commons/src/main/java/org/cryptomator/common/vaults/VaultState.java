package org.cryptomator.common.vaults;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValueBase;
import java.util.concurrent.atomic.AtomicReference;

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
		 * Unknown state due to preceeding unrecoverable exceptions.
		 */
		ERROR;
	}

	private final AtomicReference<Value> value;

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
			LOG.debug("Failed transiting into state {}: Expected state was {}, but actual state is {}.", fromState, toState, value.get());
		}
		return success;
	}

	public void set(Value newState) {
		var oldState = value.getAndSet(newState);
		if (oldState != newState) {
			fireValueChangedEvent();
		}
	}

	protected void fireValueChangedEvent() {
		if (Platform.isFxApplicationThread()) {
			super.fireValueChangedEvent();
		} else {
			Platform.runLater(super::fireValueChangedEvent);
		}
	}
}
