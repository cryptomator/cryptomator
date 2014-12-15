package org.cryptomator.ui.controls;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import org.cryptomator.ui.model.Directory;

public class DirectoryListCell extends ListCell<Directory> implements ChangeListener<Boolean> {

	// TODO: fancy graphics instead of circles ;-)
	private final Circle statusIndicator = new Circle(3.0);

	public DirectoryListCell() {
		setGraphic(statusIndicator);
		setGraphicTextGap(12.0);
		setContentDisplay(ContentDisplay.LEFT);
	}

	@Override
	protected void updateItem(Directory item, boolean empty) {
		final Directory oldItem = super.getItem();
		if (oldItem != null) {
			oldItem.unlockedProperty().removeListener(this);
		}
		super.updateItem(item, empty);
		if (item == null) {
			setText(null);
			setTooltip(null);
			statusIndicator.setVisible(false);
		} else {
			setText(item.getName());
			setTooltip(new Tooltip(item.getPath().toString()));
			statusIndicator.setVisible(true);
			item.unlockedProperty().addListener(this);
			updateStatusIndicator();
		}
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		updateStatusIndicator();
	}

	private void updateStatusIndicator() {
		final Paint statusColor = getItem().isUnlocked() ? Color.LIME : Color.RED;
		statusIndicator.setFill(statusColor);
	}

}
