package org.cryptomator.ui.notification;

import org.cryptomator.cryptofs.event.FilesystemEvent;
import org.cryptomator.event.VaultEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxNotificationRadar;

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

	private final Stage window;
	private final SimpleListProperty<VaultEvent> notificationsProp;
	private final IntegerProperty selectionIndex;
	private final ObservableStringValue paging;
	private final ObjectProperty<VaultEvent> selectedEvent;
	private final StringProperty message;
	private final StringProperty description;
	private final StringProperty actionText;
	private final ExecutorService executorService;

	@Inject
	public NotificationController(@NotificationWindow Stage window, FxNotificationRadar notificationRadar, ExecutorService executorService) {
		this.window = window;
		this.notificationsProp = new SimpleListProperty<>(notificationRadar.getEventsRequiringNotification());
		this.selectionIndex = new SimpleIntegerProperty(0);
		this.selectedEvent = new SimpleObjectProperty<>();
		selectionIndex.addListener((_, _, n) -> selectedEvent.setValue(notificationsProp.get(n.intValue())));
		selectedEvent.addListener(this::adjustTexts);
		this.paging = Bindings.createStringBinding(() -> selectionIndex.get() + 1 + "/" + notificationsProp.size(), selectionIndex, notificationsProp);
		this.message = new SimpleStringProperty();
		this.description = new SimpleStringProperty();
		this.actionText = new SimpleStringProperty();
		this.executorService = executorService;
	}

	@FXML
	public void initialize() {
		selectedEvent.setValue(notificationsProp.get(selectionIndex.get()));
	}

	//TODO: Translations!
	private void adjustTexts(ObservableValue<? extends VaultEvent> observable, VaultEvent oldEvent, VaultEvent newEvent) {
		switch (newEvent.actualEvent()) {
			default -> {
				message.set("NO CONTENT");
				description.set("IF YOU SEE THIS MESSAGE, PLEASE CONTACT THE DEVELOPERS OF CRYPTOMATOR ABOUT A BUG IN THE NOTIFICATION DISPLAY");
			}
		}
	}


	@FXML
	public void handleButtonAction() {
		try {
			var ev = selectedEvent.get();
			switch (ev.actualEvent()) {
				//TODO: executorService.submit(callback.action());
				default -> {
				} //normally nothing
			}
		} finally {
			//remove processed event
			int i = selectionIndex.get();
			notificationsProp.remove(i);
			if (notificationsProp.isEmpty()) {
				close(); //no more events
			} else if (notificationsProp.size() == i) {
				i = i - 1;
				selectionIndex.set(i); //triggers event update
			} else {
				selectedEvent.set(notificationsProp.get(i));
			}
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
		if (i != notificationsProp.size() - 1) {
			selectionIndex.set(i + 1);
		}
	}

	@FXML
	public void close() {
		notificationsProp.clear();
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

	public ObservableValue<String> actionTextProperty() {
		return actionText;
	}

	public String getActionText() {
		return Objects.requireNonNullElse(actionText.get(), "");
	}

	public ObservableBooleanValue buttonVisibleProperty() {
		return actionText.isEmpty();
	}

	public boolean isButtonVisible() {
		return actionText.get() != null;
	}

	public ObservableStringValue pagingProperty() {
		return paging;
	}

	public String getPaging() {
		return paging.get();
	}

}
