/*******************************************************************************
 * Copyright (c) 2018 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tasks {

	private static final Logger LOG = LoggerFactory.getLogger(Tasks.class);
	public static <T> TaskBuilder<T> create(Callable<T> callable) {
		return new TaskBuilder<>(callable);
	}

	public static TaskBuilder<Void> create(VoidCallable callable) {
		return create(() -> {
			callable.call();
			return null;
		});
	}

	public static class TaskBuilder<T> {

		private final Callable<T> callable;
		private Consumer<T> successHandler = x -> {
		};
		private List<ErrorHandler<?>> errorHandlers = new ArrayList<>();
		private Runnable finallyHandler = () -> {};

		TaskBuilder(Callable<T> callable) {
			this.callable = callable;
		}

		public TaskBuilder<T> onSuccess(Runnable successHandler) {
			this.successHandler = x -> {
				successHandler.run();
			};
			return this;
		}

		public TaskBuilder<T> onSuccess(Consumer<T> successHandler) {
			this.successHandler = successHandler;
			return this;
		}

		public <E extends Throwable> TaskBuilder<T> onError(Class<E> type, Consumer<E> exceptionHandler) {
			errorHandlers.add(new ErrorHandler<>(type, exceptionHandler));
			return this;
		}

		public TaskBuilder<T> andFinally(Runnable finallyHandler) {
			this.finallyHandler = finallyHandler;
			return this;
		}

		private Task<T> build() {
			return new TaskImpl<>(callable, successHandler, errorHandlers, finallyHandler);
		}

		public Task<T> runOnce(ExecutorService executorService) {
			Task<T> task = build();
			executorService.submit(task);
			return task;
		}

		public Task<T> scheduleOnce(ScheduledExecutorService executorService, long delay, TimeUnit unit) {
			Task<T> task = build();
			executorService.schedule(task, delay, unit);
			return task;
		}

		public ScheduledService<T> schedulePeriodically(ExecutorService executorService, Duration initialDelay, Duration period) {
			ScheduledService<T> service = new RestartingService<>(this::build);
			service.setExecutor(executorService);
			service.setDelay(initialDelay);
			service.setPeriod(period);
			Platform.runLater(service::start);
			return service;
		}

	}

	private static class ErrorHandler<ErrorType extends Throwable> {

		private final Class<ErrorType> type;
		private final Consumer<ErrorType> errorHandler;

		public ErrorHandler(Class<ErrorType> type, Consumer<ErrorType> errorHandler) {
			this.type = type;
			this.errorHandler = errorHandler;
		}

		public boolean handles(Throwable error) {
			return type.isInstance(error);
		}

		public void handle(Throwable error) throws ClassCastException {
			ErrorType typedError = type.cast(error);
			errorHandler.accept(typedError);
		}

	}

	/**
	 * Adapter between java.util.function.* and javafx.concurrent.*
	 */
	private static class TaskImpl<T> extends Task<T> {

		private final Callable<T> callable;
		private final Consumer<T> successHandler;
		private List<ErrorHandler<?>> errorHandlers;
		private final Runnable finallyHandler;

		TaskImpl(Callable<T> callable, Consumer<T> successHandler, List<ErrorHandler<?>> errorHandlers, Runnable finallyHandler) {
			this.callable = callable;
			this.successHandler = successHandler;
			this.errorHandlers = errorHandlers;
			this.finallyHandler = finallyHandler;
		}

		@Override
		protected T call() throws Exception {
			return callable.call();
		}

		@Override
		protected void succeeded() {
			try {
				successHandler.accept(getValue());
			} finally {
				finallyHandler.run();
			}
		}

		@Override
		protected void failed() {
			try {
				Throwable exception = getException();
				errorHandlers.stream().filter(handler -> handler.handles(exception)).findFirst().ifPresentOrElse(exceptionHandler -> {
					exceptionHandler.handle(exception);
				}, () -> {
					LOG.error("Unhandled exception", exception);
				});
			} finally {
				finallyHandler.run();
			}
		}

	}

	/**
	 * A service that keeps executing the next task long as tasks are succeeding.
	 */
	private static class RestartingService<T> extends ScheduledService<T> {

		private final Supplier<Task<T>> taskFactory;

		RestartingService(Supplier<Task<T>> taskFactory) {
			this.taskFactory = taskFactory;
		}

		@Override
		protected Task<T> createTask() {
			return taskFactory.get();
		}

	}

	public interface VoidCallable {

		void call() throws Exception;

	}

}

