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
 * Notification manager inside the UI domain.
 * <p>
 * Polls the {@link NotificationManager} for pending events every {@value POLL_INTERVAL_SECONDS } seconds and
 * triggers the notification window display when events are available.
 * Returns an observable list of events requiring a user notification with {@link #getEventsRequiringNotification()}.
 *
 * @see NotificationManager
 */
@FxApplicationScoped
public class FxNotificationManager {

	private static final int POLL_INTERVAL_SECONDS = 1;

	private final NotificationManager notificationManager;
	private final ScheduledExecutorService scheduler;
	private final FxApplicationWindows applicationWindows;
	private final ObservableList<VaultEvent> eventsRequiringNotification;

	@Inject
	public FxNotificationManager(NotificationManager notificationManager, ScheduledExecutorService scheduler, FxApplicationWindows applicationWindows) {
		this.notificationManager = notificationManager;
		this.scheduler = scheduler;
		this.applicationWindows = applicationWindows;
		this.eventsRequiringNotification = FXCollections.observableArrayList();
	}

	public void schedulePollForUpdates() {
		scheduler.scheduleAtFixedRate(this::checkForPendingNotifications, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	private void checkForPendingNotifications() {
		Platform.runLater(() -> {
			if (notificationManager.addTo(eventsRequiringNotification)) {
				applicationWindows.showNotification();
			}
		});

	}

	public ObservableList<VaultEvent> getEventsRequiringNotification() {
		return eventsRequiringNotification;
	}

}
