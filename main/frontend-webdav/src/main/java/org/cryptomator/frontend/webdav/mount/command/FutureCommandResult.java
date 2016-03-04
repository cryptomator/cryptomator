/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount.command;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cryptomator.frontend.CommandFailedException;

final class FutureCommandResult implements Future<CommandResult>, Runnable {

	private final Process process;
	private final AtomicBoolean canceled = new AtomicBoolean();
	private final AtomicBoolean done = new AtomicBoolean();
	private final Lock lock = new ReentrantLock();
	private final Condition doneCondition = lock.newCondition();

	private CommandFailedException exception;

	FutureCommandResult(Process process) {
		this.process = process;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (done.get()) {
			return false;
		} else if (canceled.compareAndSet(false, true)) {
			if (mayInterruptIfRunning) {
				process.destroyForcibly();
			}
		}
		return true;
	}

	@Override
	public boolean isCancelled() {
		return canceled.get();
	}

	private void setDone() {
		lock.lock();
		try {
			done.set(true);
			doneCondition.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isDone() {
		return done.get();
	}

	@Override
	public CommandResult get() throws InterruptedException, ExecutionException {
		lock.lock();
		try {
			while (!done.get()) {
				doneCondition.await();
			}
		} finally {
			lock.unlock();
		}
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return new CommandResult(process);
	}

	@Override
	public CommandResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		lock.lock();
		try {
			boolean doneInTime = true;
			if (!done.get()) {
				doneInTime = doneCondition.await(timeout, unit);
			}
			if (!doneInTime) {
				throw new TimeoutException();
			}
		} finally {
			lock.unlock();
		}
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return new CommandResult(process);
	}

	@Override
	public void run() {
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			exception = new CommandFailedException(e);
		} finally {
			setDone();
		}
	}

}
