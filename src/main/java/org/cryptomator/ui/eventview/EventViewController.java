package org.cryptomator.ui.eventview;

import org.cryptomator.event.Event;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.util.Comparator;

@EventViewScoped
public class EventViewController implements FxController {

	private final SortedList<Event> reversedEventList;
	private final ObservableList<Event> eventList;

	@FXML
	ListView<Event> eventListView;

	@Inject
	public EventViewController(ObservableList<Event> eventList) {
		reversedEventList = new SortedList<>(eventList, Comparator.reverseOrder());
		this.eventList = eventList;
	}

	@FXML
	public void initialize() {
		eventListView.setItems(reversedEventList);
	}

	@FXML
	void clearEventList() {
		eventList.clear();
	}


}
