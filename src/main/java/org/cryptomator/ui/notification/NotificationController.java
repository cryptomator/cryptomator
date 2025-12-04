package org.cryptomator.ui.notification;

import org.cryptomator.cryptofs.event.FilesystemEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxNotificationRadar;

import javax.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@NotificationScoped
public class NotificationController implements FxController {

	private static final String LOREM_IPSUM = """
			Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam""";

	private final Stage window;
	private final SimpleListProperty<FilesystemEvent> notificationsProp;
	private final IntegerProperty selectionIndex;
	private final ObjectProperty<FilesystemEvent> selectedEvent;
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
		selectionIndex.addListener((obs, o, n) -> selectedEvent.setValue(notificationsProp.get(n.intValue())));
		selectedEvent.addListener(this::adjustTexts);
		this.message = new SimpleStringProperty();
		this.description = new SimpleStringProperty();
		this.actionText = new SimpleStringProperty();
		this.executorService = executorService;
	}

	@FXML
	public void initialize() {
		selectedEvent.setValue(notificationsProp.get(selectionIndex.get()));
	}

	private void adjustTexts(ObservableValue<? extends FilesystemEvent> observable, FilesystemEvent oldEvent, FilesystemEvent newEvent) {
		switch (newEvent) {
			default -> {
				message.set("BABA");
				description.set(LOREM_IPSUM);
				actionText.set("ACTION");
			}
		}
	}


	@FXML
	public void handleButtonAction() {
		var ev = selectedEvent.get();
		switch (ev) {
			default -> {
			} //normally nothing
		}
		//executorService.submit(callback.action());
		int i = selectionIndex.get();
		notificationsProp.remove(i); //remove processed event

		if (notificationsProp.isEmpty()) {
			close(); //no more events
		} else if (notificationsProp.size() == i) {
			i = i - 1;
			selectionIndex.set(i); //triggers event update
		} else {
			selectedEvent.set(notificationsProp.get(i));
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

	public ObservableIntegerValue selectionIndexProperty() {
		return selectionIndex;
	}

	public int getSelectionIndex() {
		return selectionIndex.get();
	}

	public ObservableIntegerValue numberOfNotificationsProperty() {
		return notificationsProp.sizeProperty();
	}

	public int getNumberOfNotifications() {
		return notificationsProp.size();
	}

}
