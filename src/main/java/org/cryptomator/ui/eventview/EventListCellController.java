package org.cryptomator.ui.eventview;

import org.cryptomator.common.ObservableUtil;
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
import javafx.beans.property.ObjectProperty;
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
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

public class EventListCellController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(EventListCellController.class);

	private final ObservableList<VaultEvent> events;
	private final Optional<RevealPathService> revealService;
	private final ResourceBundle resourceBundle;
	private final ObjectProperty<VaultEvent> event;
	private final StringProperty eventMessage;
	private final StringProperty eventDescription;
	private final ObjectProperty<FontAwesome5Icon> eventIcon;
	private final ObservableValue<Boolean> vaultUnlocked;
	private final ObservableValue<String> message;
	private final ObservableValue<String> description;
	private final ObservableValue<FontAwesome5Icon> icon;

	@FXML
	ContextMenu eventActions;
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
		this.message = Bindings.createStringBinding(this::selectMessage, vaultUnlocked, eventMessage);
		this.description = Bindings.createStringBinding(this::selectDescription, vaultUnlocked, eventDescription);
		this.icon = Bindings.createObjectBinding(this::selectIcon, vaultUnlocked, eventIcon);
	}

	public void setEvent(@NotNull VaultEvent item) {
		event.set(item);
		eventDescription.setValue("Vault " + item.v().getDisplayName());
		eventActions.hide();
		eventActions.getItems().clear();
		addAction("generic.action.dismiss", () -> events.remove(item));
		switch (item.actualEvent()) {
			case ConflictResolvedEvent fse -> this.adjustToConflictResolvedEvent(fse);
			case ConflictResolutionFailedEvent fse -> this.adjustToConflictEvent(fse);
			case DecryptionFailedEvent fse -> this.adjustToDecryptionFailedEvent(fse);
		}
	}

	private void adjustToConflictResolvedEvent(ConflictResolvedEvent cre) {
		eventIcon.setValue(FontAwesome5Icon.FILE);
		eventMessage.setValue("Resolved conflict, new file is " + cre.resolvedCleartextPath()); //TODO:localize
		if (revealService.isPresent()) {
			addAction("event.conflictResolved.showDecrypted", () -> this.showResolvedConflict(cre));
		}
	}

	private void adjustToConflictEvent(ConflictResolutionFailedEvent cfe) {
		eventIcon.setValue(FontAwesome5Icon.TIMES);
		eventMessage.setValue("Failed to resolve conflict for " + cfe.conflictingCiphertextPath()); //TODO:localize
		if (revealService.isPresent()) {
			addAction("event.conflictFailed.showEncrypted", () -> reveal(cfe.conflictingCiphertextPath()));
		}
	}

	private void adjustToDecryptionFailedEvent(DecryptionFailedEvent dfe) {
		eventIcon.setValue(FontAwesome5Icon.BAN);
		eventMessage.setValue("Cannot decrypt " + dfe.ciphertextPath()); //TODO:localize
		if (revealService.isPresent()) {
			addAction("event.decryptionFailed.showEncrypted", () -> reveal(dfe.ciphertextPath()));
		}
	}

	private void addAction(String localizationKey, Runnable action) {
		var entry = new MenuItem(resourceBundle.getString(localizationKey));
		entry.getStyleClass().addLast("add-vault-menu-item");
		entry.setOnAction(_ -> action.run());
		eventActions.getItems().addLast(entry);
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
			return "Event for " + (e != null ? e.v().getDisplayName() : ""); //TODO: localize
		}
	}

	private String selectDescription() {
		if (vaultUnlocked.getValue()) {
			return eventDescription.getValue();
		} else {
			return "Unlock the vault to display details."; //TODO: localize
		}
	}


	@FXML
	public void toggleEventActionsMenu() {
		var e = event.get();
		if (e != null) {
			if (eventActions.isShowing()) {
				eventActions.hide();
			} else {
				eventActions.show(eventActionsButton, Side.BOTTOM, 0.0, 0.0);
			}
		}
	}

	private void showResolvedConflict(ConflictResolvedEvent cre) {
		var v = event.getValue().v();
		if (v.isUnlocked()) {
			var mountUri = v.getMountPoint().uri();
			var internalPath = cre.resolvedCleartextPath().toString().substring(1);
			var actualPath = Path.of(mountUri.getPath().concat(internalPath).substring(1));
			reveal(actualPath);
		}
	}

	private void reveal(Path p) {
		try {
			revealService.orElseThrow(() -> new IllegalStateException("Function requiring revealService called, but service not available")) //
					.reveal(p);
		} catch (RevealFailedException e) {
			LOG.warn("Failed to show path  {}",p, e);
		}
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

}
