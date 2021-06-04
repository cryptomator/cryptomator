package org.cryptomator.ui.health;

import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

class CheckListCell extends ListCell<HealthCheckTask> {

	private final FontAwesome5IconView stateIcon = new FontAwesome5IconView();

	CheckListCell() {
		setPadding(new Insets(6));
		setAlignment(Pos.CENTER_LEFT);
		setContentDisplay(ContentDisplay.LEFT);
	}

	@Override
	protected void updateItem(HealthCheckTask item, boolean empty) {
		super.updateItem(item, empty);

		if (item != null) {
			textProperty().bind(item.titleProperty());
			item.stateProperty().addListener(this::stateChanged);
			graphicProperty().bind(Bindings.createObjectBinding(() -> graphicForState(item.getState()),item.stateProperty()));
			stateIcon.setGlyph(glyphForState(item.getState()));
		} else {
			textProperty().unbind();
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
}
