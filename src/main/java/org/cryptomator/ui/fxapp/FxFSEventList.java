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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * List of all occurred filesystem events.
 * <p>
 * The list exposes an observable list to listen for updates and a property. Internally it polls the {@link FileSystemEventAggregator} in a regular interval for updates. If an update is available, the observable list is updated on the FX application thread.
 */
@FxApplicationScoped
public class FxFSEventList {

	private final ObservableList<Map.Entry<FSEventBucket, FSEventBucketContent>> events;
	private final FileSystemEventAggregator eventAggregator;
	private final ScheduledFuture<?> scheduledTask;
	private final BooleanProperty unreadEvents;

	@Inject
	public FxFSEventList(FileSystemEventAggregator fsEventAggregator, ScheduledExecutorService scheduler) {
		this.events = FXCollections.observableArrayList();
		this.eventAggregator = fsEventAggregator;
		this.unreadEvents = new SimpleBooleanProperty(false);
		this.scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
			if (fsEventAggregator.hasMaybeUpdates()) {
				flush();
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * Starts the clone task on the FX thread and wait till it is completed
	 */
	private void flush() {
		var latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			eventAggregator.cloneTo(events);
			unreadEvents.setValue(true);
			latch.countDown();
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public ObservableList<Map.Entry<FSEventBucket, FSEventBucketContent>> getObservableList() {
		return events;
	}

	public BooleanProperty unreadEventsProperty() {
		return unreadEvents;
	}


}
