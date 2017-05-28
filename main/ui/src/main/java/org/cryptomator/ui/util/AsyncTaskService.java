/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.common.ConsumerThrowingException;
import org.cryptomator.common.RunnableThrowingException;
import org.cryptomator.common.SupplierThrowingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

@Singleton
public class AsyncTaskService {

	private static final Logger LOG = LoggerFactory.getLogger(AsyncTaskService.class);

	private final ExecutorService executor;

	@Inject
	public AsyncTaskService(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Creates a new async task
	 * 
	 * @param task Tasks to be invoked in a background thread.
	 * @return The async task
	 */
	public AsyncTaskWithoutSuccessHandler<Void> asyncTaskOf(RunnableThrowingException<?> task) {
		return new AsyncTaskImpl<>(() -> {
			task.run();
			return null;
		});
	}

	/**
	 * Creates a new async task
	 * 
	 * @param task Tasks to be invoked in a background thread.
	 * @return The async task
	 */
	public <ResultType> AsyncTaskWithoutSuccessHandler<ResultType> asyncTaskOf(SupplierThrowingException<ResultType, ?> task) {
		return new AsyncTaskImpl<>(task);
	}

	private class AsyncTaskImpl<ResultType> implements AsyncTaskWithoutSuccessHandler<ResultType> {

		private final SupplierThrowingException<ResultType, ?> task;

		private ConsumerThrowingException<ResultType, ?> successHandler = value -> {
		};
		private final List<ErrorHandler<Throwable>> errorHandlers = new ArrayList<>();
		private RunnableThrowingException<?> finallyHandler = () -> {
		};

		public AsyncTaskImpl(SupplierThrowingException<ResultType, ?> task) {
			this.task = task;
		}

		@Override
		public AsyncTaskWithoutErrorHandler onSuccess(ConsumerThrowingException<ResultType, ?> handler) {
			successHandler = handler;
			return this;
		}

		@Override
		public AsyncTaskWithoutErrorHandler onSuccess(RunnableThrowingException<?> handler) {
			return onSuccess(result -> handler.run());
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public <ErrorType extends Throwable> AsyncTaskWithoutErrorHandler onError(Class<ErrorType> type, ConsumerThrowingException<ErrorType, ?> handler) {
			errorHandlers.add((ErrorHandler) new ErrorHandler<>(type, handler));
			return this;
		}

		@Override
		public <ErrorType extends Throwable> AsyncTaskWithoutErrorHandler onError(Class<ErrorType> type, RunnableThrowingException<?> handler) {
			return onError(type, error -> handler.run());
		}

		@Override
		public AsyncTask andFinally(RunnableThrowingException<?> handler) {
			finallyHandler = handler;
			return this;
		}

		@Override
		public void run() {
			errorHandlers.add(ErrorHandler.LOGGING_HANDLER);
			executor.execute(() -> logExceptions(() -> {
				try {
					ResultType result = task.get();
					Platform.runLater(() -> {
						try {
							successHandler.accept(result);
						} catch (Throwable e) {
							LOG.error("Uncaught exception", e);
						}
					});
				} catch (Throwable e) {
					ErrorHandler<Throwable> errorHandler = errorHandlerFor(e);
					Platform.runLater(toRunnableLoggingException(() -> errorHandler.accept(e)));
				} finally {
					Platform.runLater(toRunnableLoggingException(finallyHandler));
				}
			}));
		}

		private ErrorHandler<Throwable> errorHandlerFor(Throwable e) {
			return errorHandlers.stream().filter(handler -> handler.handles(e)).findFirst().get();
		}

	}

	private static Runnable toRunnableLoggingException(RunnableThrowingException<?> delegate) {
		return () -> logExceptions(delegate);
	}

	private static void logExceptions(RunnableThrowingException<?> delegate) {
		try {
			delegate.run();
		} catch (Throwable e) {
			LOG.error("Uncaught exception", e);
		}
	}

	private static class ErrorHandler<ErrorType> implements ConsumerThrowingException<ErrorType, Throwable> {

		public static final ErrorHandler<Throwable> LOGGING_HANDLER = new ErrorHandler<Throwable>(Throwable.class, error -> {
			LOG.error("Uncaught exception", error);
		});

		private final Class<ErrorType> type;
		private final ConsumerThrowingException<ErrorType, ?> delegate;

		public ErrorHandler(Class<ErrorType> type, ConsumerThrowingException<ErrorType, ?> delegate) {
			this.type = type;
			this.delegate = delegate;
		}

		public boolean handles(Throwable error) {
			return type.isInstance(error);
		}

		@Override
		public void accept(ErrorType error) throws Throwable {
			delegate.accept(error);
		}

	}

	public interface AsyncTaskWithoutSuccessHandler<ResultType> extends AsyncTaskWithoutErrorHandler {

		/**
		 * @param handler Tasks to be invoked on the JavaFX application thread.
		 * @return The async task
		 */
		AsyncTaskWithoutErrorHandler onSuccess(ConsumerThrowingException<ResultType, ?> handler);

		/**
		 * @param handler Tasks to be invoked on the JavaFX application thread.
		 * @return The async task
		 */
		AsyncTaskWithoutErrorHandler onSuccess(RunnableThrowingException<?> handler);

	}

	public interface AsyncTaskWithoutErrorHandler extends AsyncTaskWithoutFinallyHandler {

		/**
		 * @param type Exception type to catch
		 * @param handler Tasks to be invoked on the JavaFX application thread.
		 * @return The async task
		 */
		<ErrorType extends Throwable> AsyncTaskWithoutErrorHandler onError(Class<ErrorType> type, ConsumerThrowingException<ErrorType, ?> handler);

		/**
		 * @param type Exception type to catch
		 * @param handler Tasks to be invoked on the JavaFX application thread.
		 * @return The async task
		 */
		<ErrorType extends Throwable> AsyncTaskWithoutErrorHandler onError(Class<ErrorType> type, RunnableThrowingException<?> handler);

	}

	public interface AsyncTaskWithoutFinallyHandler extends AsyncTask {

		/**
		 * @param handler Tasks to be invoked on the JavaFX application thread.
		 * @return The async task
		 */
		AsyncTask andFinally(RunnableThrowingException<?> handler);

	}

	public interface AsyncTask extends Runnable {

		/**
		 * Starts the async task.
		 */
		@Override
		void run();
	}

}
