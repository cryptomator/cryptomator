package org.cryptomator.ui.common;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserInteractionLock<E extends Enum> {

	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();
	private final BooleanProperty awaitingInteraction = new SimpleBooleanProperty();
	private volatile E state;

	public UserInteractionLock(E initialValue) {
		state = initialValue;
	}

	public void interacted(E result) {
		assert Platform.isFxApplicationThread();
		lock.lock();
		try {
			state = result;
			awaitingInteraction.set(false);
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	public E awaitInteraction() throws InterruptedException {
		assert !Platform.isFxApplicationThread();
		lock.lock();
		try {
			Platform.runLater(() -> awaitingInteraction.set(true));
			condition.await();
			return state;
		} finally {
			lock.unlock();
		}
	}

	public ReadOnlyBooleanProperty awaitingInteraction() {
		return awaitingInteraction;
	}

}
