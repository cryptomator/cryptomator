package org.cryptomator.ui.fxapp;

import org.cryptomator.event.FileSystemEventRegistry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@FxApplicationScoped
public class EventsUpdateCheck {

	private final ObservableList<Map.Entry<FileSystemEventRegistry.Key, FileSystemEventRegistry.Value>> events;
	private final FileSystemEventRegistry eventRegistry;
	private final ScheduledFuture<?> scheduledTask;
	private final BooleanProperty unreadEvents;

	@Inject
	public EventsUpdateCheck(FileSystemEventRegistry eventRegistry, ScheduledExecutorService scheduler, @Named("unreadEventsAvailable") BooleanProperty unreadEvents) {
		this.events = FXCollections.observableArrayList();
		this.eventRegistry = eventRegistry;
		this.unreadEvents = unreadEvents;
		this.scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
			if (eventRegistry.hasUpdates()) {
				flush();
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
		//TODO: allow the task to be canceled (to enable ui actions, e.g. when the contextMenu is open, the list should not be updated
	}

	public ObservableList<Map.Entry<FileSystemEventRegistry.Key, FileSystemEventRegistry.Value>> getList() {
		return events;
	}

	/**
	 * Clones the registry into the observable list
	 */
	private void flush() {
		var latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			eventRegistry.cloneTo(events);
			unreadEvents.setValue(true);
			latch.countDown();
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}



}
