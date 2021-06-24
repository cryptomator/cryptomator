package org.cryptomator.ui.health;

import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.util.Callback;

class CheckListCell extends ListCell<HealthCheckTask> {

	private final FontAwesome5IconView stateIcon = new FontAwesome5IconView();
	private final Callback<HealthCheckTask, BooleanProperty> selectedGetter;
	private final ObjectProperty<State> stateProperty;

	private CheckBox checkBox = new CheckBox();
	private BooleanProperty selectedProperty;

	CheckListCell(Callback<HealthCheckTask, BooleanProperty> selectedGetter, ObservableValue<Boolean> switchIndicator) {
		this.selectedGetter = selectedGetter;
		this.stateProperty = new SimpleObjectProperty<>(State.SELECTION);
		switchIndicator.addListener(this::changeState);
		setPadding(new Insets(6));
		setAlignment(Pos.CENTER_LEFT);
		setContentDisplay(ContentDisplay.LEFT);
		getStyleClass().add("label");
	}

	private void changeState(ObservableValue<? extends Boolean> observableValue, boolean oldValue, boolean newValue) {
		if (newValue) {
			stateProperty.set(State.RUN);
		} else {
			stateProperty.set(State.SELECTION);
		}
	}

	@Override
	protected void updateItem(HealthCheckTask item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.getTitle());
		}
		switch (stateProperty.get()) {
			case SELECTION -> updateItemSelection(item, empty);
			case RUN -> updateItemRun(item, empty);
		}
	}

	private void updateItemSelection(HealthCheckTask item, boolean empty) {
		if (!empty) {
			setGraphic(checkBox);

			if (selectedProperty != null) {
				checkBox.selectedProperty().unbindBidirectional(selectedProperty);
			}
			selectedProperty = selectedGetter.call(item);
			if (selectedProperty != null) {
				checkBox.selectedProperty().bindBidirectional(selectedProperty);
			}
		} else {
			setGraphic(null);
			setText(null);
		}
	}

	private void updateItemRun(HealthCheckTask item, boolean empty) {
		if (item != null) {
			item.stateProperty().addListener(this::stateChanged);
			graphicProperty().bind(Bindings.createObjectBinding(() -> graphicForState(item.getState()), item.stateProperty()));
			stateIcon.setGlyph(glyphForState(item.getState()));
		} else {
			graphicProperty().unbind();
			setGraphic(null);
			setText(null);
		}
	}

	private void stateChanged(ObservableValue<? extends Worker.State> observable, Worker.State oldState, Worker.State newState) {
		stateIcon.setGlyph(glyphForState(newState));
		stateIcon.setVisible(true);
	}

	private Node graphicForState(Worker.State state) {
		return switch (state) {
			case READY -> null;
			case SCHEDULED, RUNNING, FAILED, CANCELLED, SUCCEEDED -> stateIcon;
		};
	}

	private FontAwesome5Icon glyphForState(Worker.State state) {
		return switch (state) {
			case READY -> FontAwesome5Icon.COG; //just a placeholder
			case SCHEDULED -> FontAwesome5Icon.CLOCK;
			case RUNNING -> FontAwesome5Icon.SPINNER;
			case FAILED -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
			case CANCELLED -> FontAwesome5Icon.BAN;
			case SUCCEEDED -> FontAwesome5Icon.CHECK;
		};
	}

	private enum State {
		SELECTION,
		RUN;
	}
}
