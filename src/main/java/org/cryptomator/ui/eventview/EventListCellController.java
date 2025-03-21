package org.cryptomator.ui.eventview;

import org.cryptomator.event.FSEventBucket;
import org.cryptomator.event.FSEventBucketContent;
import org.cryptomator.event.FileSystemEventAggregator;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.cryptofs.CryptoPath;
import org.cryptomator.cryptofs.event.BrokenDirFileEvent;
import org.cryptomator.cryptofs.event.BrokenFileNodeEvent;
import org.cryptomator.cryptofs.event.ConflictResolutionFailedEvent;
import org.cryptomator.cryptofs.event.ConflictResolvedEvent;
import org.cryptomator.cryptofs.event.DecryptionFailedEvent;
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
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

public class EventListCellController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(EventListCellController.class);
	private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private final FileSystemEventAggregator fileSystemEventAggregator;
	@Nullable
	private final RevealPathService revealService;
	private final ResourceBundle resourceBundle;
	private final ObjectProperty<Map.Entry<FSEventBucket, FSEventBucketContent>> eventEntry;
	private final StringProperty eventMessage;
	private final StringProperty eventDescription;
	private final ObjectProperty<FontAwesome5Icon> eventIcon;
	private final ObservableValue<String> eventCount;
	private final ObservableValue<Boolean> vaultUnlocked;
	private final ObservableValue<String> readableTime;
	private final ObservableValue<String> readableDate;
	private final ObservableValue<String> message;
	private final ObservableValue<String> description;
	private final ObservableValue<FontAwesome5Icon> icon;
	private final BooleanProperty actionsButtonVisible;
	private final Tooltip eventTooltip;

	@FXML
	HBox root;
	@FXML
	ContextMenu eventActionsMenu;
	@FXML
	Button eventActionsButton;

	@Inject
	public EventListCellController(FileSystemEventAggregator fileSystemEventAggregator, Optional<RevealPathService> revealService, ResourceBundle resourceBundle) {
		this.fileSystemEventAggregator = fileSystemEventAggregator;
		this.revealService = revealService.orElseGet(() -> null);
		this.resourceBundle = resourceBundle;
		this.eventEntry = new SimpleObjectProperty<>(null);
		this.eventMessage = new SimpleStringProperty();
		this.eventDescription = new SimpleStringProperty();
		this.eventIcon = new SimpleObjectProperty<>();
		this.eventCount = ObservableUtil.mapWithDefault(eventEntry, e -> e.getValue().count() == 1? "" : "("+ e.getValue().count() +")", "");
		this.vaultUnlocked = ObservableUtil.mapWithDefault(eventEntry.flatMap(e -> e.getKey().vault().unlockedProperty()), Function.identity(), false);
		this.readableTime = ObservableUtil.mapWithDefault(eventEntry, e -> LOCAL_TIME_FORMATTER.format(e.getValue().mostRecentEvent().getTimestamp()), "");
		this.readableDate = ObservableUtil.mapWithDefault(eventEntry, e -> LOCAL_DATE_FORMATTER.format(e.getValue().mostRecentEvent().getTimestamp()), "");
		this.message = Bindings.createStringBinding(this::selectMessage, vaultUnlocked, eventMessage);
		this.description = Bindings.createStringBinding(this::selectDescription, vaultUnlocked, eventDescription);
		this.icon = Bindings.createObjectBinding(this::selectIcon, vaultUnlocked, eventIcon);
		this.actionsButtonVisible = new SimpleBooleanProperty();
		this.eventTooltip = new Tooltip();
		eventTooltip.setShowDelay(Duration.millis(500.0));
	}

	@FXML
	public void initialize() {
		actionsButtonVisible.bind(Bindings.createBooleanBinding(this::determineActionsButtonVisibility, root.hoverProperty(), eventActionsMenu.showingProperty(), vaultUnlocked));
		vaultUnlocked.addListener((_, _, newValue) -> eventActionsMenu.hide());
		Tooltip.install(root, eventTooltip);
	}

	private boolean determineActionsButtonVisibility() {
		return vaultUnlocked.getValue() && (eventActionsMenu.isShowing() || root.isHover());
	}

	public void setEventEntry(@NotNull Map.Entry<FSEventBucket, FSEventBucketContent> item) {
		eventEntry.set(item);
		eventActionsMenu.hide();
		eventActionsMenu.getItems().clear();
		eventTooltip.setText(item.getKey().vault().getDisplayName());
		addAction("generic.action.dismiss", () -> {
			fileSystemEventAggregator.remove(item.getKey());
		});
		switch (item.getValue().mostRecentEvent()) {
			case ConflictResolvedEvent fse -> this.adjustToConflictResolvedEvent(fse);
			case ConflictResolutionFailedEvent fse -> this.adjustToConflictEvent(fse);
			case DecryptionFailedEvent fse -> this.adjustToDecryptionFailedEvent(fse);
			case BrokenDirFileEvent fse -> this.adjustToBrokenDirFileEvent(fse);
			case BrokenFileNodeEvent fse -> this.adjustToBrokenFileNodeEvent(fse);
		}
	}


	private void adjustToBrokenFileNodeEvent(BrokenFileNodeEvent bfe) {
		eventIcon.setValue(FontAwesome5Icon.TIMES);
		eventMessage.setValue(resourceBundle.getString("eventView.entry.brokenFileNode.message"));
		eventDescription.setValue(bfe.ciphertextPath().getFileName().toString());
		if (revealService != null) {
			addAction("eventView.entry.brokenFileNode.showEncrypted", () -> reveal(revealService, convertVaultPathToSystemPath(bfe.ciphertextPath())));
		} else {
			addAction("eventView.entry.brokenFileNode.copyEncrypted", () -> copyToClipboard(convertVaultPathToSystemPath(bfe.ciphertextPath()).toString()));
		}
		addAction("eventView.entry.brokenFileNode.copyDecrypted", () -> copyToClipboard(convertVaultPathToSystemPath(bfe.cleartextPath()).toString()));
	}

	private void adjustToConflictResolvedEvent(ConflictResolvedEvent cre) {
		eventIcon.setValue(FontAwesome5Icon.CHECK);
		eventMessage.setValue(resourceBundle.getString("eventView.entry.conflictResolved.message"));
		eventDescription.setValue(cre.resolvedCiphertextPath().getFileName().toString());
		if (revealService != null) {
			addAction("eventView.entry.conflictResolved.showDecrypted", () -> reveal(revealService, convertVaultPathToSystemPath(cre.resolvedCleartextPath())));
		} else {
			addAction("eventView.entry.conflictResolved.copyDecrypted", () -> copyToClipboard(convertVaultPathToSystemPath(cre.resolvedCleartextPath()).toString()));
		}
	}

	private void adjustToConflictEvent(ConflictResolutionFailedEvent cfe) {
		eventIcon.setValue(FontAwesome5Icon.COMPRESS_ALT);
		eventMessage.setValue(resourceBundle.getString("eventView.entry.conflict.message"));
		eventDescription.setValue(cfe.conflictingCiphertextPath().getFileName().toString());
		if (revealService != null) {
			addAction("eventView.entry.conflict.showDecrypted", () -> reveal(revealService, convertVaultPathToSystemPath(cfe.canonicalCleartextPath())));
			addAction("eventView.entry.conflict.showEncrypted", () -> reveal(revealService, cfe.conflictingCiphertextPath()));
		} else {
			addAction("eventView.entry.conflict.copyDecrypted", () -> copyToClipboard(convertVaultPathToSystemPath(cfe.canonicalCleartextPath()).toString()));
			addAction("eventView.entry.conflict.copyEncrypted", () -> copyToClipboard(cfe.conflictingCiphertextPath().toString()));
		}
	}

	private void adjustToDecryptionFailedEvent(DecryptionFailedEvent dfe) {
		eventIcon.setValue(FontAwesome5Icon.BAN);
		eventMessage.setValue(resourceBundle.getString("eventView.entry.decryptionFailed.message"));
		eventDescription.setValue(dfe.ciphertextPath().getFileName().toString());
		if (revealService != null) {
			addAction("eventView.entry.decryptionFailed.showEncrypted", () -> reveal(revealService, dfe.ciphertextPath()));
		} else {
			addAction("eventView.entry.decryptionFailed.copyEncrypted", () -> copyToClipboard(dfe.ciphertextPath().toString()));
		}
	}

	private void adjustToBrokenDirFileEvent(BrokenDirFileEvent bde) {
		eventIcon.setValue(FontAwesome5Icon.TIMES);
		eventMessage.setValue(resourceBundle.getString("eventView.entry.brokenDirFile.message"));
		eventDescription.setValue(bde.ciphertextPath().getParent().getFileName().toString());
		if (revealService != null) {
			addAction("eventView.entry.brokenDirFile.showEncrypted", () -> reveal(revealService, bde.ciphertextPath()));
		} else {
			addAction("eventView.entry.brokenDirFile.copyEncrypted", () -> copyToClipboard(bde.ciphertextPath().toString()));
		}
	}

	private void addAction(String localizationKey, Runnable action) {
		var entry = new MenuItem(resourceBundle.getString(localizationKey));
		entry.getStyleClass().addLast("dropdown-button-context-menu-item");
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
			return resourceBundle.getString("eventView.entry.vaultLocked.message");
		}
	}

	private String selectDescription() {
		if (vaultUnlocked.getValue()) {
			return eventDescription.getValue();
		} else if (eventEntry.getValue() != null) {
			var e = eventEntry.getValue().getKey();
			return resourceBundle.getString("eventView.entry.vaultLocked.description").formatted(e != null ? e.vault().getDisplayName() : "");
		} else {
			return "";
		}
	}


	@FXML
	public void toggleEventActionsMenu() {
		var e = eventEntry.get();
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
		var v = eventEntry.getValue().getKey().vault();
		if (!v.isUnlocked()) {
			return Path.of(System.getProperty("user.home"));
		}

		var mountUri = v.getMountPoint().uri();
		var internalPath = p.toString().substring(1);
		return Path.of(mountUri.getPath().concat(internalPath).substring(1));
	}

	private void reveal(RevealPathService s, Path p) {
		try {
			s.reveal(p);
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

	public ObservableValue<String> countProperty() {
		return eventCount;
	}

	public String getCount() {
		return eventCount.getValue();
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

	public ObservableValue<Boolean> vaultUnlockedProperty() {
		return vaultUnlocked;
	}

	public boolean isVaultUnlocked() {
		return vaultUnlocked.getValue();
	}
}
