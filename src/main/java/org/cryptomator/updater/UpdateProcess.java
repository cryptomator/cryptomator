package org.cryptomator.updater;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface UpdateProcess {

	/**
	 * A thread-safe method to check the progress of the update preparation.
	 * @return a value between 0.0 and 1.0 indicating the progress of the update preparation.
	 */
	double preparationProgress();

	/**
	 * Cancels the update process and cleans up any resources that were used during the preparation.
	 */
	void cancel();

	/**
	 * Blocks the current thread until the update preparation is complete or an error occurs.
	 * <p>
	 * If the preparation is already complete, this method returns immediately.
	 *
	 * @throws InterruptedException if the current thread is interrupted while waiting.
	 */
	void await() throws InterruptedException;

	/**
	 * Blocks the current thread until the update preparation is complete or an error occurs, or until the specified timeout expires.
	 * <p>
	 * If the preparation is already complete, this method returns immediately.
	 *
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the {@code timeout} argument
	 * @return true if the update is prepared
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * Once the update preparation is complete, this method can be called to launch the external update process.
	 * <p>
	 * This method shall be called after making sure that the application is ready to be restarted, e.g. after locking all vaults.
	 *
	 * @return a {@link Process} that represents the external update process.
	 * @throws IllegalStateException if the update preparation is not complete or if the update process cannot be launched.
	 * @throws IOException if starting the update process fails
	 */
	Process applyUpdate() throws IllegalStateException, IOException;


}
