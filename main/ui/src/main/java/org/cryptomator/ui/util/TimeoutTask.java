/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

/**
 * A task which is supposed to be repeated until it succeeds.
 * 
 * @author Tillmann Gaida
 *
 * @param <E>
 *            The type of checked exception that this task may throw.
 */
public interface TimeoutTask<E extends Exception> {
	/**
	 * Attempts to execute the task.
	 * 
	 * @param timeout
	 *            The time remaining to finish the task.
	 * @return true if the task finished, false if it needs to be attempted
	 *         again.
	 * @throws E
	 * @throws InterruptedException
	 */
	boolean attempt(long timeout) throws E, InterruptedException;

	/**
	 * Attempts a task until a timeout occurs. Checks for this timeout are based
	 * on {@link System#currentTimeMillis()}, so they are very crude. The task
	 * is guaranteed to be attempted once.
	 * 
	 * @param task
	 *            the task to perform.
	 * @param timeout
	 *            time in millis before this method stops attempting to finish
	 *            the task. greater than zero.
	 * @param sleepTimes
	 *            time in millis to sleep between attempts. greater than zero.
	 * @return true if the task was finished, false if the task never always
	 *         returned false or as soon as the task throws an
	 *         {@link InterruptedException}.
	 * @throws E
	 *             From the task.
	 */
	public static <E extends Exception> boolean attempt(TimeoutTask<E> task, long timeout, long sleepTimes) throws E {
		if (timeout <= 0 || sleepTimes <= 0) {
			throw new IllegalArgumentException();
		}

		long currentTime = System.currentTimeMillis();

		long tryUntil = currentTime + timeout;

		for (;; currentTime = System.currentTimeMillis()) {
			if (currentTime >= tryUntil) {
				return false;
			}

			try {
				if (task.attempt(tryUntil - currentTime)) {
					return true;
				}

				currentTime = System.currentTimeMillis();

				if (currentTime + sleepTimes < tryUntil) {
					Thread.sleep(sleepTimes);
				} else {
					return false;
				}
			} catch (InterruptedException e) {
				return false;
			}
		}
	}
}