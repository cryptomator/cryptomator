package org.cryptomator.ui.fxapp;

import org.cryptomator.event.NotificationManager;
import org.cryptomator.event.VaultEvent;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends notifications
 */
@FxApplicationScoped
public class FxNotificationRadar {

	private final NotificationManager notificationManager;
	private final ScheduledExecutorService scheduler;
	private final FxApplicationWindows applicationWindows;
	private final ObservableList<VaultEvent> eventsRequiringNotification;

	@Inject
	public FxNotificationRadar(NotificationManager notificationManager, ScheduledExecutorService scheduler, FxApplicationWindows applicationWindows) {
		this.notificationManager = notificationManager;
		this.scheduler = scheduler;
		this.applicationWindows = applicationWindows;
		this.eventsRequiringNotification = FXCollections.observableArrayList();
	}

	public void schedulePollForUpdates() {
		scheduler.scheduleAtFixedRate(this::checkForPendingNotifications, 0, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * TODO
	 */
	private void checkForPendingNotifications() {
		Platform.runLater(() -> {
			if (notificationManager.cloneTo(eventsRequiringNotification)) {
				applicationWindows.showNotification();
			}
		});

	}

	public ObservableList<VaultEvent> getEventsRequiringNotification() {
		return eventsRequiringNotification;
	}

}
