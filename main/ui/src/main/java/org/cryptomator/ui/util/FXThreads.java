/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * https://github.com/totalvoidness/FXThreads
 * 
 * Contributors:
 *     Sebastian Stenzel
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javafx.application.Platform;

/**
 * Use this utility class to spawn background tasks and wait for them to finish. <br/>
 * <br/>
 * <strong>Example use (ignoring exceptions):</strong>
 * 
 * <pre>
 * // get some string from a remote server:
 * Future&lt;String&gt; futureBookName = runOnBackgroundThread(restResource::getBookName);
 * 
 * // when done, update text label:
 * runOnMainThreadWhenFinished(futureBookName, (bookName) -&gt; {
 * 	myLabel.setText(bookName);
 * });
 * </pre>
 * 
 * <strong>Example use (exception-aware):</strong>
 * 
 * <pre>
 * // get some string from a remote server:
 * Future&lt;String&gt; futureBookName = runOnBackgroundThread(restResource::getBookName);
 * 
 * // when done, update text label:
 * runOnMainThreadWhenFinished(futureBookName, (bookName) -&gt; {
 * 	myLabel.setText(bookName);
 * }, (exception) -&gt; {
 * 	myLabel.setText(&quot;An exception occured: &quot; + exception.getMessage());
 * });
 * </pre>
 */
public final class FXThreads {

	private static final CallbackWhenTaskFailed DUMMY_EXCEPTION_CALLBACK = (e) -> {
		// ignore.
	};

	/**
	 * Waits for the given task to complete and notifies the given successCallback. If an exception occurs, the callback will never be
	 * called. If you are interested in the exception, use
	 * {@link #runOnMainThreadWhenFinished(ExecutorService, Future, CallbackWhenTaskFinished, CallbackWhenTaskFailed)} instead.
	 * 
	 * <pre>
	 * // example:
	 * 
	 * runOnMainThreadWhenFinished(futureBookName, (bookName) -&gt; {
	 * 	myLabel.setText(bookName);
	 * });
	 * </pre>
	 * @param executor
	 * @param task The task to wait for.
	 * @param successCallback The action to perform, when the task finished.
	 */
	public static <T> void runOnMainThreadWhenFinished(ExecutorService executor, Future<T> task, CallbackWhenTaskFinished<T> successCallback) {
		executor.submit(() -> {
			return "asd";
		});
		runOnMainThreadWhenFinished(executor, task, successCallback, DUMMY_EXCEPTION_CALLBACK);
	}

	/**
	 * Waits for the given task to complete and notifies the given successCallback. If an exception occurs, the callback will never be
	 * called. If you are interested in the exception, use
	 * {@link #runOnMainThreadWhenFinished(ExecutorService, Future, CallbackWhenTaskFinished, CallbackWhenTaskFailed)} instead.
	 * 
	 * <pre>
	 * // example:
	 * 
	 * runOnMainThreadWhenFinished(futureBookNamePossiblyFailing, (bookName) -&gt; {
	 * 	myLabel.setText(bookName);
	 * }, (exception) -&gt; {
	 * 	myLabel.setText(&quot;An exception occured: &quot; + exception.getMessage());
	 * });
	 * </pre>
	 * 
	 * @param executor
	 *            The service to execute the background task on
	 * @param task The task to wait for.
	 * @param successCallback The action to perform, when the task finished.
	 * @param exceptionCallback
	 */
	public static <T> void runOnMainThreadWhenFinished(ExecutorService executor, Future<T> task, CallbackWhenTaskFinished<T> successCallback, CallbackWhenTaskFailed exceptionCallback) {
		Objects.requireNonNull(task, "task must not be null.");
		Objects.requireNonNull(successCallback, "successCallback must not be null.");
		Objects.requireNonNull(exceptionCallback, "exceptionCallback must not be null.");
		executor.execute(() -> {
			try {
				final T result = task.get();
				Platform.runLater(() -> {
					successCallback.taskFinished(result);
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					exceptionCallback.taskFailed(e);
				});
			}
		});
	}

	public interface CallbackWhenTaskFinished<T> {
		void taskFinished(T result);
	}

	public interface CallbackWhenTaskFailed {
		void taskFailed(Throwable t);
	}

}
