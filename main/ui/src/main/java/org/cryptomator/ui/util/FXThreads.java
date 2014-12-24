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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
	private static final CallbackWhenTaskFailed DUMMY_EXCEPTION_CALLBACK = (e) -> {
		// ignore.
	};

	private FXThreads() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Executes the given task on a background thread. If you want to react on the result on your JavaFX main thread, use
	 * {@link #runOnMainThreadWhenFinished(Future, CallbackWhenTaskFinished)}.
	 * 
	 * <pre>
	 * // examples:
	 * 
	 * Future&lt;String&gt; futureBookName1 = runOnBackgroundThread(restResource::getBookName);
	 * 
	 * Future&lt;String&gt; futureBookName2 = runOnBackgroundThread(() -&gt; {
	 * 	return restResource.getBookName();
	 * });
	 * </pre>
	 * 
	 * @param task The task to be executed on a background thread.
	 * @return A future result object, which you can use in {@link #runOnMainThreadWhenFinished(Future, CallbackWhenTaskFinished)}.
	 */
	public static <T> Future<T> runOnBackgroundThread(Callable<T> task) {
		return EXECUTOR.submit(task);
	}

	/**
	 * Executes the given task on a background thread. If you want to react on the result on your JavaFX main thread, use
	 * {@link #runOnMainThreadWhenFinished(Future, CallbackWhenTaskFinished)}.
	 * 
	 * <pre>
	 * // examples:
	 * 
	 * Future&lt;?&gt; futureDone1 = runOnBackgroundThread(this::doSomeComplexCalculation);
	 * 
	 * Future&lt;?&gt; futureDone2 = runOnBackgroundThread(() -&gt; {
	 * 	doSomeComplexCalculation();
	 * });
	 * </pre>
	 * 
	 * @param task The task to be executed on a background thread.
	 * @return A future result object, which you can use in {@link #runOnMainThreadWhenFinished(Future, CallbackWhenTaskFinished)}.
	 */
	public static Future<?> runOnBackgroundThread(Runnable task) {
		return EXECUTOR.submit(task);
	}

	/**
	 * Waits for the given task to complete and notifies the given successCallback. If an exception occurs, the callback will never be
	 * called. If you are interested in the exception, use
	 * {@link #runOnMainThreadWhenFinished(Future, CallbackWhenTaskFinished, CallbackWhenTaskFailed)} instead.
	 * 
	 * <pre>
	 * // example:
	 * 
	 * runOnMainThreadWhenFinished(futureBookName, (bookName) -&gt; {
	 * 	myLabel.setText(bookName);
	 * });
	 * </pre>
	 * 
	 * @param task The task to wait for.
	 * @param successCallback The action to perform, when the task finished.
	 */
	public static <T> void runOnMainThreadWhenFinished(Future<T> task, CallbackWhenTaskFinished<T> successCallback) {
		runOnBackgroundThread(() -> {
			return "asd";
		});
		FXThreads.runOnMainThreadWhenFinished(task, successCallback, DUMMY_EXCEPTION_CALLBACK);
	}

	/**
	 * Waits for the given task to complete and notifies the given successCallback. If an exception occurs, the callback will never be
	 * called. If you are interested in the exception, use
	 * {@link #runOnMainThreadWhenFinished(Future, CallbackWhenTaskFinished, CallbackWhenTaskFailed)} instead.
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
	 * @param task The task to wait for.
	 * @param successCallback The action to perform, when the task finished.
	 */
	public static <T> void runOnMainThreadWhenFinished(Future<T> task, CallbackWhenTaskFinished<T> successCallback, CallbackWhenTaskFailed exceptionCallback) {
		assertParamNotNull(task, "task must not be null.");
		assertParamNotNull(successCallback, "successCallback must not be null.");
		assertParamNotNull(exceptionCallback, "exceptionCallback must not be null.");
		EXECUTOR.execute(() -> {
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

	private static void assertParamNotNull(Object param, String msg) {
		if (param == null) {
			throw new IllegalArgumentException(msg);
		}
	}

	public interface CallbackWhenTaskFinished<T> {
		void taskFinished(T result);
	}

	public interface CallbackWhenTaskFailed {
		void taskFailed(Throwable t);
	}

}
