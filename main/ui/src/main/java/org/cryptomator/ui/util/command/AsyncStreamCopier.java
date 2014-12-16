/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.ui.util.command;

import static org.apache.commons.io.IOUtils.copy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class AsyncStreamCopier implements Runnable {

	private final Lock lock = new ReentrantLock();
	private final Condition doneCondition = lock.newCondition();
	private final InputStream input;
	private final OutputStream output;

	private IOException exception;
	private boolean done;

	public AsyncStreamCopier(InputStream input, OutputStream output) {
		this.input = input;
		this.output = output;
	}

	@Override
	public void run() {
		lock.lock();
		try (InputStream inputToBeClosed = input; OutputStream outputToBeClosed = output) {
			copy(input, output);
			done = true;
			doneCondition.signal();
		} catch (IOException e) {
			exception = e;
		} finally {
			lock.unlock();
		}
	}

	private void waitUntilDone() {
		lock.lock();
		try {
			while (!done) {
				doneCondition.await();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
	}

	public void assertOk() throws IOException {
		this.waitUntilDone();
		if (exception != null) {
			throw exception;
		}
	}

}
