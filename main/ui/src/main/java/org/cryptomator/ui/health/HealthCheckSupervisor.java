package org.cryptomator.ui.health;

import dagger.Lazy;

import javax.inject.Inject;
import javafx.concurrent.Task;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class HealthCheckSupervisor extends Task<Void> {

	private final ExecutorService executor;
	private final Lazy<Collection<HealthCheckTask>> lazyTasks;
	private final Runnable masterkeyDestructor;
	private final AtomicReference<HealthCheckTask> currentTask;
	private final ConcurrentLinkedQueue<HealthCheckTask> remainingTasks;

	@Inject
	public HealthCheckSupervisor(Lazy<Collection<HealthCheckTask>> lazyTasks, Runnable masterkeyDestructor) {
		this.lazyTasks = lazyTasks;
		this.masterkeyDestructor = masterkeyDestructor;
		this.currentTask = new AtomicReference<>(null);
		this.executor = Executors.newSingleThreadExecutor();
		this.remainingTasks = new ConcurrentLinkedQueue<>();
	}

	public Void call() {
		remainingTasks.addAll(lazyTasks.get());
		while (!remainingTasks.isEmpty()) {
			final var task = remainingTasks.remove();
			currentTask.set(task);
			executor.execute(task); //TODO: think about if we set the scheduled property for all tasks?
			try {
				task.get();
			} catch (InterruptedException e) {
				executor.shutdownNow();
				cleanup();
				;
				Thread.currentThread().interrupt(); //TODO: do we need this?
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (CancellationException e) {
				// ok
			}
		}
		return null;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		cleanup();
		currentTask.get().cancel(mayInterruptIfRunning);
		if (mayInterruptIfRunning) {
			executor.shutdownNow();
		} else {
			executor.shutdown();
		}
		return super.cancel(mayInterruptIfRunning);
	}

	private void cleanup() {
		remainingTasks.forEach(task -> task.cancel(false));
		remainingTasks.clear();
	}

	@Override
	protected void done() {
		masterkeyDestructor.run(); //TODO: if we destroy it, no check can rerun
	}

	public Collection<HealthCheckTask> getTasks() {
		return lazyTasks.get();
	}
}
