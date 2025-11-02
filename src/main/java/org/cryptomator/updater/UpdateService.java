package org.cryptomator.updater;

import org.cryptomator.integrations.update.UpdateMechanism;
import org.cryptomator.integrations.update.UpdateStep;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A service that performs all update steps provided by the given {@link UpdateMechanism} in sequence.
 */
public class UpdateService extends Service<UpdateStep> {

	private ObservableValue<UpdateMechanism> updateMechanism;

	public UpdateService(ObservableValue<UpdateMechanism> updateMechanism) {
		setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		this.updateMechanism = updateMechanism;
	}

	@Override
	protected Task<UpdateStep> createTask() {
		return new RunAllStepsTask(updateMechanism.getValue());
	}

	private static class RunAllStepsTask extends Task<UpdateStep> {

		private final UpdateMechanism updateMechanism;

		public RunAllStepsTask(UpdateMechanism updateMechanism) {
			this.updateMechanism = Objects.requireNonNull(updateMechanism);
		}

		@Override
		protected UpdateStep call() throws IOException {
			try {
				UpdateStep step = updateMechanism.firstStep();
				UpdateStep lastStep;
				do {
					step.start();
					observeAndWaitFor(step);
					lastStep = step;
					step = step.nextStep();
				} while (step != null);
				return lastStep;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("Update interrupted");
			}
		}

		private void observeAndWaitFor(UpdateStep step) throws InterruptedException {
			do {
				updateProgress(step.preparationProgress(), 1.0);
				updateMessage(step.description());
			} while (!step.await(100, TimeUnit.MILLISECONDS));
		}
	}

}
