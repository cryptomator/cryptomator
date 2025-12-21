package org.cryptomator.ui.notification;

import org.cryptomator.common.Constants;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.cryptofs.event.FileIsInUseEvent;
import org.cryptomator.event.VaultEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxNotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@NotificationScoped
public class NotificationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);
	private static final DateTimeFormatter LOCAL_TIME_FORMATTER_TEMPLATE = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final String BUG_MSG = "If you see this message, please report it on the Cryptomator issue tracker.";

	private final Stage window;
	private final SimpleListProperty<VaultEvent> events;
	private final ResourceBundle resourceBundle;
	private final IntegerProperty selectionIndex;
	private final ObservableStringValue paging;
	private final ObjectProperty<VaultEvent> selectedEvent;
	private final ObservableValue<Boolean> singleEvent;
	private final StringProperty fileName;
	private final StringProperty vaultName;
	private final StringProperty eventTimestamp;
	private final StringProperty message;
	private final StringProperty description;
	private final StringProperty actionText;
	private final ExecutorService executorService;
	private final DateTimeFormatter localizedTimeFormatter;

	@Inject
	public NotificationController(@NotificationWindow Stage window, FxNotificationManager notificationManager, ExecutorService executorService, ResourceBundle resourceBundle, Settings settings) {
		this.window = window;
		var preferredLanguage = settings.language.get();
		this.localizedTimeFormatter = LOCAL_TIME_FORMATTER_TEMPLATE.withLocale(preferredLanguage == null ? Locale.getDefault() : Locale.forLanguageTag(preferredLanguage));
		this.events = new SimpleListProperty<>(notificationManager.getEventsRequiringNotification());
		this.resourceBundle = resourceBundle;
		this.selectionIndex = new SimpleIntegerProperty(-1);
		this.selectedEvent = new SimpleObjectProperty<>();
		this.singleEvent = events.sizeProperty().map(size -> size.intValue() == 1);
		this.paging = Bindings.createStringBinding(() -> selectionIndex.get() + 1 + "/" + events.size(), selectionIndex, events);
		this.vaultName = new SimpleStringProperty();
		this.eventTimestamp = new SimpleStringProperty();
		this.message = new SimpleStringProperty();
		this.fileName = new SimpleStringProperty("");
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

	private void selectTexts(ObservableValue<? extends VaultEvent> observable, VaultEvent oldEvent, VaultEvent newEvent) {
		if (newEvent == null) {
			vaultName.set("");
			message.set("NO CONTENT");
			description.set(BUG_MSG);
			actionText.set(null);
			return;
		}

		vaultName.set(newEvent.v().getDisplayName());
		switch (newEvent.actualEvent()) {
			case FileIsInUseEvent fiiue -> {
				var userAndDevice = fiiue.owner().split(Constants.HUB_USER_DEVICE_SEPARATOR);
				var user = userAndDevice[0];
				var device = userAndDevice.length == 1 ? userAndDevice[0] : userAndDevice[1];
				var cleartextFileName = fiiue.cleartextPath().substring(fiiue.cleartextPath().lastIndexOf('/') + 1);
				eventTimestamp.set(localizedTimeFormatter.format(fiiue.lastUpdated()));
				message.set("File is opened on another device");
				fileName.set(cleartextFileName);
				description.set("The file is opened by %s on device %s. Ask the user to close the file and sync again. Otherwise, you can ignore the lock and open it anyway.".formatted(user, device));
				actionText.set("Ignore Lock");
				/* TODO: Once feature is out of beta, activate translations
				message.set(resourceBundle.getString("notification.inUse.message"));
				description.set(resourceBundle.getString("notification.inUse.description").formatted(fiiue.cleartextPath(), user, device));
				actionText.set(resourceBundle.getString("notification.inUse.action"));
				 */
			}
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
				case FileIsInUseEvent fiiue -> {
					executorService.submit(fiiue.ignoreMethod());
				}
				default -> {
				} //normally nothing
			}
		} finally {
			removeSelectedEvent();
		}
	}

	private void removeSelectedEvent() {
		int i = selectionIndex.get();
		var size = events.size();
		if (i < 0 || i >= size) {
			LOG.error("Selection index {} is out of bounds of list size {} during event removal. Closing Window.", i, size);
			window.close();
			return;
		}

		events.remove(i);
		if (events.isEmpty()) {
			window.close(); //no more events
		} else if (events.size() == i) {
			selectionIndex.set(i - 1); //triggers event update
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
	public ObservableValue<String> eventTimeProperty() {
		return eventTimestamp;
	}

	public String getEventTime() {
		return eventTimestamp.get();
	}

	public ObservableValue<String> vaultNameProperty() {
		return vaultName;
	}

	public String getVaultName() {
		return vaultName.get();
	}

	public ObservableValue<String> fileNameProperty() {
		return fileName;
	}

	public String getFileName() {
		return fileName.get();
	}

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

	public ObservableValue<Boolean> singleEventProperty() {
		return singleEvent;
	}

	public boolean isSingleEvent() {
		return singleEvent.getValue();
	}

}
