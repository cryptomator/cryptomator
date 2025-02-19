package org.cryptomator.ui.eventview;

import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.ConflictResolvedEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;
import org.cryptomator.event.Event;
import org.cryptomator.event.UpdateEvent;
import org.cryptomator.event.VaultEvent;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

public class EventListCellController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(EventListCellController.class);

	private final ObservableList<Event> events;
	private final Optional<RevealPathService> revealService;
	private final ResourceBundle resourceBundle;
	private final ObjectProperty<Event> event;
	private final ObservableValue<String> message;
	private final ObservableValue<String> description;
	private final ObservableValue<FontAwesome5Icon> icon;

	@FXML
	ContextMenu basicEventActions;
	@FXML
	ContextMenu conflictResoledEventActions;
	@FXML
	Button eventActionsButton;

	@Inject
	public EventListCellController(ObservableList<Event> events, Optional<RevealPathService> revealService, ResourceBundle resourceBundle) {
		this.events = events;
		this.revealService = revealService;
		this.resourceBundle = resourceBundle;
		this.event = new SimpleObjectProperty<>(null);
		this.message = ObservableUtil.mapWithDefault(event, e -> e.getClass().getName(), "");
		this.description = ObservableUtil.mapWithDefault(event, this::selectDescription, "");
		this.icon = ObservableUtil.mapWithDefault(event, this::selectIcon, FontAwesome5Icon.BELL);
		event.addListener(this::hideContextMenus);
	}


	private void hideContextMenus(Observable observable, Event oldValue, Event newValue) {
		basicEventActions.hide();
		conflictResoledEventActions.hide();
	}

	public void setEvent(Event item) {
		event.set(item);
	}

	private FontAwesome5Icon selectIcon(Event e) {
		return switch (e) {
			case UpdateEvent _ -> FontAwesome5Icon.BELL;
			case VaultEvent _ -> FontAwesome5Icon.FILE;
		};
	}

	private String selectDescription(Event e) {
		return switch (e) {
			case UpdateEvent(_, String newVersion) -> resourceBundle.getString("preferences.updates.updateAvailable").formatted(newVersion);
			case VaultEvent _ -> "A vault is weird!";
		};
	}

	@FXML
	public void toggleEventActionsMenu() {
		var e = event.get();
		if (e != null) {
			var contextMenu = switch (e) {
				case VaultEvent _ -> conflictResoledEventActions;
				default -> basicEventActions;
			};
			if (contextMenu.isShowing()) {
				contextMenu.hide();
			} else {
				contextMenu.show(eventActionsButton, Side.BOTTOM, 0.0, 0.0);
			}
		}
	}

	@FXML
	public void dismissEvent() {
		events.remove(event.getValue());
	}

	@FXML
	public void showResolvedConflict() {
		if (event.getValue() instanceof VaultEvent(_, Vault v, FilesystemEvent fse) && fse instanceof ConflictResolvedEvent cre) {
			if (v.isUnlocked()) {
				var mountUri = v.getMountPoint().uri();
				var internalPath = cre.resolvedCleartextPath().toString().substring(1);
				var actualPath = Path.of(mountUri.getPath().concat(internalPath).substring(1));
				var s = revealService.orElseThrow(() -> new IllegalStateException("Function requiring revealService called, but service not available"));
				try {
					s.reveal(actualPath);
				} catch (RevealFailedException e) {
					LOG.warn("Failed to show resolved file conflict", e);
				}

			}
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

	public boolean isRevealServicePresent() {
		return revealService.isPresent();
	}

}
