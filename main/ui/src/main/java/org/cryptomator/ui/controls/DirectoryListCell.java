/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controls;

import org.cryptomator.ui.model.Vault;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class DirectoryListCell extends DraggableListCell<Vault>implements ChangeListener<Boolean> {

	// fill: #FD4943, stroke: #E1443F
	private static final Color RED_FILL = Color.rgb(253, 73, 67);
	private static final Color RED_STROKE = Color.rgb(225, 68, 63);

	// fill: #28CA40, stroke: #30B740
	private static final Color GREEN_FILL = Color.rgb(40, 202, 64);
	private static final Color GREEN_STROKE = Color.rgb(48, 183, 64);

	private final Circle statusIndicator = new Circle(4.5);
	private final Label nameText = new Label();
	private final Label pathText = new Label();
	private final VBox vbox = new VBox(4.0, nameText, pathText);
	private final HBox hbox = new HBox(6.0, statusIndicator, vbox);
	private ContextMenu vaultContextMenu;

	public DirectoryListCell() {
		hbox.setAlignment(Pos.CENTER_LEFT);
		hbox.setPrefWidth(1);
		vbox.setFillWidth(true);

		nameText.textFillProperty().bind(this.textFillProperty());
		nameText.fontProperty().bind(this.fontProperty());

		pathText.setTextOverrun(OverrunStyle.ELLIPSIS);
		pathText.getStyleClass().add("detail-label");

		setGraphic(hbox);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	@Override
	protected void updateItem(Vault item, boolean empty) {
		final Vault oldItem = super.getItem();
		if (oldItem != null) {
			oldItem.unlockedProperty().removeListener(this);
		}
		super.updateItem(item, empty);
		if (item == null) {
			nameText.setText(null);
			pathText.setText(null);
			setTooltip(null);
			setContextMenu(null);
			statusIndicator.setVisible(false);
		} else {
			nameText.setText(item.getName());
			pathText.setText(item.getDisplayablePath());
			setTooltip(new Tooltip(item.getPath().toString()));
			statusIndicator.setVisible(true);
			item.unlockedProperty().addListener(this);
			updateStatusIndicator();
			updateContextMenu();
		}
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		updateStatusIndicator();
		updateContextMenu();
	}

	private void updateStatusIndicator() {
		final Paint fillColor = getItem().isUnlocked() ? GREEN_FILL : RED_FILL;
		final Paint strokeColor = getItem().isUnlocked() ? GREEN_STROKE : RED_STROKE;
		statusIndicator.setFill(fillColor);
		statusIndicator.setStroke(strokeColor);
	}

	private void updateContextMenu() {
		if (getItem().isUnlocked()) {
			this.setContextMenu(null);
		} else {
			this.setContextMenu(vaultContextMenu);
		}
	}

	public void setVaultContextMenu(ContextMenu contextMenu) {
		this.vaultContextMenu = contextMenu;
	}

}
