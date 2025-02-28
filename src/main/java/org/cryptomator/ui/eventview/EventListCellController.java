package org.cryptomator.ui.eventview;

import org.cryptomator.common.ObservableUtil;
import org.cryptomator.cryptofs.CryptoPath;
import org.cryptomator.cryptofs.event.ConflictResolutionFailedEvent;
import org.cryptomator.cryptofs.event.ConflictResolvedEvent;
import org.cryptomator.cryptofs.event.DecryptionFailedEvent;
import org.cryptomator.event.VaultEvent;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

public class EventListCellController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(EventListCellController.class);
	private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private final ObservableList<VaultEvent> events;
	private final Optional<RevealPathService> revealService;
	private final ResourceBundle resourceBundle;
	private final ObjectProperty<VaultEvent> event;
	private final StringProperty eventMessage;
	private final StringProperty eventDescription;
	private final ObjectProperty<FontAwesome5Icon> eventIcon;
	private final ObservableValue<Boolean> vaultUnlocked;
	private final ObservableValue<String> readableTime;
	private final ObservableValue<String> readableDate;
	private final ObservableValue<String> message;
	private final ObservableValue<String> description;
	private final ObservableValue<FontAwesome5Icon> icon;
	private final BooleanProperty actionsButtonVisible;

	@FXML
	HBox root;
	@FXML
	ContextMenu eventActionsMenu;
	@FXML
	Button eventActionsButton;

	@Inject
	public EventListCellController(ObservableList<VaultEvent> events, Optional<RevealPathService> revealService, ResourceBundle resourceBundle) {
		this.events = events;
		this.revealService = revealService;
		this.resourceBundle = resourceBundle;
		this.event = new SimpleObjectProperty<>(null);
		this.eventMessage = new SimpleStringProperty();
		this.eventDescription = new SimpleStringProperty();
		this.eventIcon = new SimpleObjectProperty<>();
		this.vaultUnlocked = ObservableUtil.mapWithDefault(event.flatMap(e -> e.v().unlockedProperty()), Function.identity(), false);
		this.readableTime = ObservableUtil.mapWithDefault(event, e -> LOCAL_TIME_FORMATTER.format(e.timestamp()), "");
		this.readableDate = ObservableUtil.mapWithDefault(event, e -> LOCAL_DATE_FORMATTER.format(e.timestamp()), "");
		this.message = Bindings.createStringBinding(this::selectMessage, vaultUnlocked, eventMessage);
		this.description = Bindings.createStringBinding(this::selectDescription, vaultUnlocked, eventDescription);
		this.icon = Bindings.createObjectBinding(this::selectIcon, vaultUnlocked, eventIcon);
		this.actionsButtonVisible = new SimpleBooleanProperty();
	}

	@FXML
	public void initialize() {
		actionsButtonVisible.bind(Bindings.createBooleanBinding(this::determineActionsButtonVisibility, root.hoverProperty(), eventActionsMenu.showingProperty(), vaultUnlocked));
		vaultUnlocked.addListener((_, _, newValue) -> eventActionsMenu.hide());
	}

	private boolean determineActionsButtonVisibility() {
		return vaultUnlocked.getValue() && (eventActionsMenu.isShowing() || root.isHover());
	}

	public void setEvent(@NotNull VaultEvent item) {
		event.set(item);
		eventActionsMenu.hide();
		eventActionsMenu.getItems().clear();
		addAction("generic.action.dismiss", () -> events.remove(item));
		switch (item.actualEvent()) {
			case ConflictResolvedEvent fse -> this.adjustToConflictResolvedEvent(fse);
			case ConflictResolutionFailedEvent fse -> this.adjustToConflictEvent(fse);
			case DecryptionFailedEvent fse -> this.adjustToDecryptionFailedEvent(fse);
		}
	}

	private void adjustToConflictResolvedEvent(ConflictResolvedEvent cre) {
		eventIcon.setValue(FontAwesome5Icon.FILE);
		eventMessage.setValue(cre.resolvedCleartextPath().toString());
		eventDescription.setValue(resourceBundle.getString("event.conflictResolved.description"));
		if (revealService.isPresent()) {
			addAction("event.conflictResolved.showDecrypted", () -> reveal(convertVaultPathToSystemPath(cre.resolvedCleartextPath())));
		} else {
			addAction("event.conflictResolved.copyDecrypted", () -> copyToClipboard(convertVaultPathToSystemPath(cre.resolvedCleartextPath()).toString()));
		}
	}

	private void adjustToConflictEvent(ConflictResolutionFailedEvent cfe) {
		eventIcon.setValue(FontAwesome5Icon.TIMES);
		eventMessage.setValue(cfe.canonicalCleartextPath().toString());
		eventDescription.setValue(resourceBundle.getString("event.conflict.description"));
		if (revealService.isPresent()) {
			addAction("event.conflict.showDecrypted", () -> reveal(convertVaultPathToSystemPath(cfe.canonicalCleartextPath())));
			addAction("event.conflict.showEncrypted", () -> reveal(cfe.conflictingCiphertextPath()));
		} else {
			addAction("event.conflict.copyDecrypted", () -> copyToClipboard(convertVaultPathToSystemPath(cfe.canonicalCleartextPath()).toString()));
			addAction("event.conflict.copyEncrypted", () -> copyToClipboard(cfe.conflictingCiphertextPath().toString()));
		}
	}

	private void adjustToDecryptionFailedEvent(DecryptionFailedEvent dfe) {
		eventIcon.setValue(FontAwesome5Icon.BAN);
		eventMessage.setValue(dfe.ciphertextPath().toString());
		eventDescription.setValue(resourceBundle.getString("event.decryptionFailed.description"));
		if (revealService.isPresent()) {
			addAction("event.decryptionFailed.showEncrypted", () -> reveal(dfe.ciphertextPath()));
		} else {
			addAction("event.decryptionFailed.copyEncrypted", () -> copyToClipboard(dfe.ciphertextPath().toString()));
		}
	}

	private void addAction(String localizationKey, Runnable action) {
		var entry = new MenuItem(resourceBundle.getString(localizationKey));
		entry.getStyleClass().addLast("add-vault-menu-item");
		entry.setOnAction(_ -> action.run());
		eventActionsMenu.getItems().addLast(entry);
	}


	private FontAwesome5Icon selectIcon() {
		if (vaultUnlocked.getValue()) {
			return eventIcon.getValue();
		} else {
			return FontAwesome5Icon.LOCK;
		}
	}

	private String selectMessage() {
		if (vaultUnlocked.getValue()) {
			return eventMessage.getValue();
		} else {
			var e = event.getValue();
			return resourceBundle.getString("event.vaultLocked.message");
		}
	}

	private String selectDescription() {
		if (vaultUnlocked.getValue()) {
			return eventDescription.getValue();
		} else {
			return resourceBundle.getString("event.vaultLocked.description");
		}
	}


	@FXML
	public void toggleEventActionsMenu() {
		var e = event.get();
		if (e != null) {
			if (eventActionsMenu.isShowing()) {
				eventActionsMenu.hide();
			} else {
				eventActionsMenu.show(eventActionsButton, Side.BOTTOM, 0.0, 0.0);
			}
		}
	}

	private Path convertVaultPathToSystemPath(Path p) {
		if (!(p instanceof CryptoPath)) {
			throw new IllegalArgumentException("Path " + p + " is not a vault path");
		}
		var v = event.getValue().v();
		if (!v.isUnlocked()) {
			return Path.of(System.getProperty("user.home"));
		}

		var mountUri = v.getMountPoint().uri();
		var internalPath = p.toString().substring(1);
		return Path.of(mountUri.getPath().concat(internalPath).substring(1));
	}

	private void reveal(Path p) {
		try {
			revealService.orElseThrow(() -> new IllegalStateException("Function requiring revealService called, but service not available")) //
					.reveal(p);
		} catch (RevealFailedException e) {
			LOG.warn("Failed to show path  {}", p, e);
		}
	}

	private void copyToClipboard(String s) {
		var content = new ClipboardContent();
		content.putString(s);
		Clipboard.getSystemClipboard().setContent(content);
	}

	//-- property accessors --
	public ObservableValue<String> messageProperty() {
		return message;
	}

	public String getMessage() {
		return message.getValue();
	}

	public ObservableValue<String> descriptionProperty() {
		return description;
	}

	public String getDescription() {
		return description.getValue();
	}

	public ObservableValue<FontAwesome5Icon> iconProperty() {
		return icon;
	}

	public FontAwesome5Icon getIcon() {
		return icon.getValue();
	}

	public ObservableValue<Boolean> actionsButtonVisibleProperty() {
		return actionsButtonVisible;
	}

	public boolean isActionsButtonVisible() {
		return actionsButtonVisible.getValue();
	}

	public ObservableValue<String> eventLocalTimeProperty() {
		return readableTime;
	}

	public String getEventLocalTime() {
		return readableTime.getValue();
	}

	public ObservableValue<String> eventLocalDateProperty() {
		return readableDate;
	}

	public String getEventLocalDate() {
		return readableDate.getValue();
	}
}
