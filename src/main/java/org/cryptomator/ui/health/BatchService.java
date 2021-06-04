package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import dagger.Lazy;

import javax.inject.Inject;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class BatchService extends Service<Void> {

	private final Iterator<HealthCheckTask> remainingTasks;

	@Inject
	public BatchService(Iterable<HealthCheckTask> tasks) {
		this.remainingTasks = tasks.iterator();
	}

	@Override
	protected Task<Void> createTask() {
		Preconditions.checkState(remainingTasks.hasNext(), "No remaining tasks");
		return remainingTasks.next();
	}

	@Override
	protected void succeeded() {
		if (remainingTasks.hasNext()) {
			this.restart();
		}
	}
}
