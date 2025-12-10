package org.cryptomator.ui.notification;

import org.cryptomator.event.VaultEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxNotificationManager;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@NotificationScoped
public class NotificationController implements FxController {

	private static final String BUG_MSG = "IF YOU SEE THIS MESSAGE, PLEASE CONTACT THE DEVELOPERS OF CRYPTOMATOR ABOUT A BUG IN THE NOTIFICATION DISPLAY";

	private final Stage window;
	private final SimpleListProperty<VaultEvent> events;
	private final IntegerProperty selectionIndex;
	private final ObservableStringValue paging;
	private final ObjectProperty<VaultEvent> selectedEvent;
	private final StringProperty message;
	private final StringProperty description;
	private final StringProperty actionText;
	private final ExecutorService executorService;

	@Inject
	public NotificationController(@NotificationWindow Stage window, FxNotificationManager notificationManager, ExecutorService executorService) {
		this.window = window;
		this.events = new SimpleListProperty<>(notificationManager.getEventsRequiringNotification());
		this.selectionIndex = new SimpleIntegerProperty(-1);
		this.selectedEvent = new SimpleObjectProperty<>();
		this.paging = Bindings.createStringBinding(() -> selectionIndex.get() + 1 + "/" + events.size(), selectionIndex, events);
		this.message = new SimpleStringProperty();
		this.description = new SimpleStringProperty();
		this.actionText = new SimpleStringProperty();
		this.executorService = executorService;
	}

	@FXML
	public void initialize() {
		selectionIndex.addListener((_, _, n) -> {
			if (!events.isEmpty()) {
				selectedEvent.setValue(events.get(n.intValue()));
			}
		});
		selectedEvent.addListener(this::selectTexts);

		selectionIndex.setValue(0);
	}

	//TODO: Translations!
	private void selectTexts(ObservableValue<? extends VaultEvent> observable, VaultEvent oldEvent, VaultEvent newEvent) {
		if (newEvent == null) {
			message.set("NO CONTENT");
			description.set(BUG_MSG);
			actionText.set(null);
			return;
		}

		switch (newEvent.actualEvent()) {
			default -> {
				message.set("NO CONTENT");
				description.set(BUG_MSG);
				actionText.set(null);
			}
		}
	}


	@FXML
	public void processSelectedEvent() {
		try {
			var ev = selectedEvent.get();
			switch (ev.actualEvent()) {
				//TODO: executorService.submit(callback.action());
				default -> {
				} //normally nothing
			}
		} finally {
			removeSelectedEvent();
		}
	}

	private void removeSelectedEvent() {
		int i = selectionIndex.get();
		events.remove(i);
		if (events.isEmpty()) {
			close(); //no more events
		} else if (events.size() == i) {
			selectionIndex.set(i-1); //triggers event update
		} else {
			selectedEvent.set(events.get(i));
		}
	}

	@FXML
	public void previousNotification() {
		int i = selectionIndex.get();
		if (i != 0) {
			selectionIndex.set(i - 1);
		}
	}

	@FXML
	public void nextNotification() {
		int i = selectionIndex.get();
		if (i != events.size() - 1) {
			selectionIndex.set(i + 1);
		}
	}

	@FXML
	public void close() {
		events.clear();
		window.close();
	}


	//FXML bindings
	public ObservableValue<String> messageProperty() {
		return message;
	}

	public String getMessage() {
		return message.get();
	}

	public ObservableValue<String> descriptionProperty() {
		return description;
	}

	public String getDescription() {
		return description.get();
	}

	public StringProperty actionTextProperty() {
		return actionText;
	}

	public String getActionText() {
		return Objects.requireNonNullElse(actionText.get(), "");
	}

	public ObservableStringValue pagingProperty() {
		return paging;
	}

	public String getPaging() {
		return paging.get();
	}

}
