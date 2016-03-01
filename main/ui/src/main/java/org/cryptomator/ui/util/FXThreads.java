/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
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
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

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
 * } , (exception) -&gt; {
 * 	myLabel.setText(&quot;An exception occured: &quot; + exception.getMessage());
 * });
 * </pre>
 */
public final class FXThreads {

	private static final Consumer<Exception> DUMMY_EXCEPTION_CALLBACK = (e) -> {
		// ignore.
	};

	/**
	 * Waits for the given task to complete and notifies the given successCallback. If an exception occurs, the callback will never be called. If you are interested in the exception, use
	 * {@link #runOnMainThreadWhenFinished(ExecutorService, Future, CallbackWhenTaskFinished, CallbackWhenTaskFailed)} instead.
	 * 
	 * <pre>
	 * // example:
	 * 
	 * runOnMainThreadWhenFinished(futureBookName, (bookName) -&gt; {
	 * 	myLabel.setText(bookName);
	 * });
	 * </pre>
	 * 
	 * @param executor The service to execute the background task on
	 * @param task The task to wait for.
	 * @param successCallback The action to perform, when the task finished.
	 */
	public static <T> void runOnMainThreadWhenFinished(ExecutorService executor, Future<T> task, Consumer<T> successCallback) {
		runOnMainThreadWhenFinished(executor, task, successCallback, DUMMY_EXCEPTION_CALLBACK);
	}

	/**
	 * Waits for the given task to complete and notifies the given successCallback. If an exception occurs, the callback will never be called. If you are interested in the exception, use
	 * {@link #runOnMainThreadWhenFinished(ExecutorService, Future, CallbackWhenTaskFinished, CallbackWhenTaskFailed)} instead.
	 * 
	 * <pre>
	 * // example:
	 * 
	 * runOnMainThreadWhenFinished(futureBookNamePossiblyFailing, (bookName) -&gt; {
	 * 	myLabel.setText(bookName);
	 * } , (exception) -&gt; {
	 * 	myLabel.setText(&quot;An exception occured: &quot; + exception.getMessage());
	 * });
	 * </pre>
	 * 
	 * @param executor The service to execute the background task on
	 * @param task The task to wait for.
	 * @param successCallback The action to perform, when the task finished.
	 * @param exceptionCallback
	 */
	public static <T> void runOnMainThreadWhenFinished(ExecutorService executor, Future<T> task, Consumer<T> successCallback, Consumer<Exception> exceptionCallback) {
		Objects.requireNonNull(task, "task must not be null.");
		Objects.requireNonNull(successCallback, "successCallback must not be null.");
		Objects.requireNonNull(exceptionCallback, "exceptionCallback must not be null.");
		executor.execute(() -> {
			try {
				final T result = task.get();
				Platform.runLater(() -> {
					successCallback.accept(result);
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					exceptionCallback.accept(e);
				});
			}
		});
	}

	public static <E> ObservableSet<E> observableSetOnMainThread(ObservableSet<E> set) {
		return new ObservableSetOnMainThread<E>(set);
	}

	public static <E> ObservableList<E> observableListOnMainThread(ObservableList<E> list) {
		return new ObservableListOnMainThread<E>(list);
	}

}
