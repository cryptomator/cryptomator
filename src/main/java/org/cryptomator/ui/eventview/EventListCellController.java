package org.cryptomator.ui.eventview;

import org.cryptomator.common.ObservableUtil;
import org.cryptomator.event.Event;
import org.cryptomator.event.UpdateEvent;
import org.cryptomator.event.VaultEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.HBox;
import java.util.ResourceBundle;

public class EventListCellController implements FxController {

	private final ResourceBundle resourceBundle;
	private final ObjectProperty<Event> event;
	private final ObservableValue<String> message;
	private final ObservableValue<String> description;
	private final ObservableValue<FontAwesome5Icon> icon;

	public FontAwesome5IconView eventIcon;
	public HBox eventListCell;

	@Inject
	public EventListCellController(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
		this.event = new SimpleObjectProperty<>(null);
		this.message = ObservableUtil.mapWithDefault(event, e -> e.getClass().getName(),"");
		this.description = ObservableUtil.mapWithDefault(event, this::selectDescription,"");
		this.icon = ObservableUtil.mapWithDefault(event, this::selectIcon, FontAwesome5Icon.BELL);
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
			case UpdateEvent(_,String newVersion) -> resourceBundle.getString("preferences.updates.updateAvailable").formatted(newVersion);
			case VaultEvent _ -> "A vault is weird!";
		};
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
