package org.cryptomator.ui.eventview;

import org.cryptomator.common.ObservableUtil;
import org.cryptomator.event.Event;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.HBox;

public class EventListCellController implements FxController {

	private final ObjectProperty<Event> event;
	private final ObservableValue<String> description;

	public FontAwesome5IconView eventIcon;
	public HBox eventListCell;

	@Inject
	public EventListCellController() {
		this.event = new SimpleObjectProperty<>(null);
		this.description = ObservableUtil.mapWithDefault(event, e -> e.getClass().getName(),"");
	}

	public void setEvent(Event item) {
		event.set(item);
	}

	public ObservableValue<String> descriptionProperty() {
		return description;
	}

	public String getDescription() {
		return description.getValue();
	}
}
