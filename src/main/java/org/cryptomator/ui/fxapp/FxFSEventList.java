package org.cryptomator.ui.fxapp;

import org.cryptomator.event.FSEventBucket;
import org.cryptomator.event.FSEventBucketContent;
import org.cryptomator.event.FileSystemEventAggregator;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * List of all occurred filesystem events.
 * <p>
 * The list exposes an observable list to listen for updates and a property. Internally it polls the {@link FileSystemEventAggregator} in a regular interval for updates. If an update is available, the observable list is updated on the FX application thread.
 */
@FxApplicationScoped
public class FxFSEventList {

	private final ObservableList<Map.Entry<FSEventBucket, FSEventBucketContent>> events;
	private final FileSystemEventAggregator eventAggregator;
	private final ScheduledService<?> pollService;
	private final BooleanProperty unreadEvents;

	@Inject
	public FxFSEventList(FileSystemEventAggregator fsEventAggregator, ScheduledExecutorService scheduler) {
		this.events = FXCollections.observableArrayList();
		this.eventAggregator = fsEventAggregator;
		this.unreadEvents = new SimpleBooleanProperty(false);
		this.pollService = new ScheduledService<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new PollTask();
			}
		};
		pollService.setDelay(Duration.millis(1000));
		pollService.setPeriod(Duration.millis(1000));
		pollService.setExecutor(Platform::runLater);
	}

	public void startPolling() {
		pollService.start();
	}

	/**
	 * Clones the aggregated events to the UI list. Must be executed on the FX thread.
	 */
	private class PollTask extends Task<Void> {

		@Override
		protected Void call() {
			if (eventAggregator.hasMaybeUpdates()) {
				eventAggregator.cloneTo(events);
				unreadEvents.setValue(true);
			}
			return null;
		}
	}

	public ObservableList<Map.Entry<FSEventBucket, FSEventBucketContent>> getObservableList() {
		return events;
	}

	public BooleanProperty unreadEventsProperty() {
		return unreadEvents;
	}


}
