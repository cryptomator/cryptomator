package org.cryptomator.ui.controls;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import org.cryptomator.ui.model.Vault;

public class DirectoryListCell extends DraggableListCell<Vault> implements ChangeListener<Boolean> {

	// fill: #FD4943, stroke: #E1443F
	private static final Color RED_FILL = Color.rgb(253, 73, 67);
	private static final Color RED_STROKE = Color.rgb(225, 68, 63);

	// fill: #28CA40, stroke: #30B740
	private static final Color GREEN_FILL = Color.rgb(40, 202, 64);
	private static final Color GREEN_STROKE = Color.rgb(48, 183, 64);

	private final Circle statusIndicator = new Circle(4.5);

	public DirectoryListCell() {
		setGraphic(statusIndicator);
		setGraphicTextGap(12.0);
		setContentDisplay(ContentDisplay.LEFT);
	}

	@Override
	protected void updateItem(Vault item, boolean empty) {
		final Vault oldItem = super.getItem();
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
		final Paint fillColor = getItem().isUnlocked() ? GREEN_FILL : RED_FILL;
		final Paint strokeColor = getItem().isUnlocked() ? GREEN_STROKE : RED_STROKE;
		statusIndicator.setFill(fillColor);
		statusIndicator.setStroke(strokeColor);
	}

}
