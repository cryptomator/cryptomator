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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * List of all occurred filesystem events.
 * <p>
 * The list exposes an observable list and a property to listen for updates. Internally it polls the {@link FileSystemEventAggregator} in a regular interval for updates.
 * If an update is available, the list from the {@link FileSystemEventAggregator } is cloned to this list on the FX application thread.
 */
@FxApplicationScoped
public class FxFSEventList {

	private final ObservableList<Map.Entry<FSEventBucket, FSEventBucketContent>> events;
	private final FileSystemEventAggregator eventAggregator;
	private final ScheduledExecutorService scheduler;
	private final BooleanProperty unreadEvents;

	@Inject
	public FxFSEventList(FileSystemEventAggregator fsEventAggregator, ScheduledExecutorService scheduler) {
		this.events = FXCollections.observableArrayList();
		this.eventAggregator = fsEventAggregator;
		this.scheduler = scheduler;
		this.unreadEvents = new SimpleBooleanProperty(false);
	}

	public void schedulePollForUpdates() {
		scheduler.schedule(this::checkForEventUpdates, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * Checks for event updates and reschedules.
	 * If updates are available, the aggregated events are copied from back- to the frontend.
	 * Reschedules itself on successful execution
	 */
	private void checkForEventUpdates() {
		if (eventAggregator.hasMaybeUpdates()) {
			Platform.runLater(() -> {
				eventAggregator.cloneTo(events);
				unreadEvents.setValue(true);
				schedulePollForUpdates();
			});
		} else {
			schedulePollForUpdates();
		}
	}

	public ObservableList<Map.Entry<FSEventBucket, FSEventBucketContent>> getObservableList() {
		return events;
	}

	public BooleanProperty unreadEventsProperty() {
		return unreadEvents;
	}
}
