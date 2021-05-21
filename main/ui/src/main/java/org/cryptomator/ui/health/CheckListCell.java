package org.cryptomator.ui.health;

import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

class CheckListCell extends ListCell<HealthCheckTask> {

	private final FontAwesome5IconView stateIcon = new FontAwesome5IconView();

	CheckListCell(){
		paddingProperty().set(new Insets(6));
	}

	@Override
	protected void updateItem(HealthCheckTask item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.getCheck().identifier());
			item.stateProperty().addListener(this::stateChanged);
			setGraphic(stateIcon);
			stateIcon.setGlyph(glyphForState(item.getState()));
			if (item.getState() == Worker.State.READY) {
				stateIcon.setVisible(false);
			}
			setContentDisplay(ContentDisplay.LEFT);
		} else {
			setText(null);
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}
	}

	private void stateChanged(ObservableValue<? extends Worker.State> observable, Worker.State oldState, Worker.State newState) {
		stateIcon.setGlyph(glyphForState(newState));
		stateIcon.setVisible(true);
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
