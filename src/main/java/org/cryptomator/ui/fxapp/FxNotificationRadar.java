package org.cryptomator.ui.fxapp;

import javax.inject.Inject;

/**
 * Scans the event list for events requiring a notification
 */
@FxApplicationScoped
public class FxNotificationRadar {

	private final FxFSEventList eventList;

	@Inject
	FxNotificationRadar(FxFSEventList eventList) {
		this.eventList = eventList;
		eventList.getObservableList()

	}


}
