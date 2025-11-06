package org.cryptomator.updater;

import org.cryptomator.integrations.update.UpdateInfo;
import org.cryptomator.integrations.update.UpdateMechanism;
import org.cryptomator.integrations.update.UpdateStep;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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

	private final BooleanBinding updateFailed = Bindings.equal(State.FAILED, stateProperty());

	private ObservableValue<UpdateInfo<?>> updateInfo;

	public UpdateService(ObservableValue<UpdateInfo<?>> updateInfo) {
		setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		this.updateInfo = updateInfo;
	}

	@Override
	protected Task<UpdateStep> createTask() {
		return new RunAllStepsTask(updateInfo.getValue());
	}

	private static class RunAllStepsTask extends Task<UpdateStep> {

		@SuppressWarnings("rawtypes")
		private final UpdateInfo updateInfo;

		public RunAllStepsTask(UpdateInfo<?> updateInfo) {
			this.updateInfo = Objects.requireNonNull(updateInfo);
		}

		@Override
		protected UpdateStep call() throws IOException {
			try {
				UpdateStep step = updateInfo.useToPrepareFirstStep();
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

	/* Observable Properties */

	public boolean isUpdateFailed() {
		return updateFailed.get();
	}

	public BooleanBinding updateFailedProperty() {
		return updateFailed;
	}
}
