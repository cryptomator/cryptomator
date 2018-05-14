/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controls;

import org.cryptomator.ui.model.Vault;
import org.fxmisc.easybind.EasyBind;

import javafx.beans.binding.ObjectExpression;
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

public class DirectoryListCell extends DraggableListCell<Vault> {

	private static final Color UNLOCKED_ICON_COLOR = new Color(0.901, 0.494, 0.133, 1.0);
	private final Label statusText = new Label();
	private final Label nameText = new Label();
	private final Label pathText = new Label();
	private final VBox vbox = new VBox(4.0, nameText, pathText);
	private final HBox hbox = new HBox(6.0, statusText, vbox);

	private ContextMenu vaultContextMenu;

	public DirectoryListCell() {
		ObjectExpression<Vault.State> vaultState = ObjectExpression.objectExpression(EasyBind.select(itemProperty()).selectObject(Vault::stateProperty));

		hbox.setAlignment(Pos.CENTER_LEFT);
		hbox.setPrefWidth(1);
		vbox.setFillWidth(true);

		nameText.textProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::name));
		nameText.textFillProperty().bind(this.textFillProperty());
		nameText.fontProperty().bind(this.fontProperty());

		pathText.textProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::displayablePath));
		pathText.setTextOverrun(OverrunStyle.ELLIPSIS);
		pathText.getStyleClass().add("detail-label");

		statusText.textProperty().bind(EasyBind.map(vaultState, this::getStatusIconText));
		statusText.textFillProperty().bind(EasyBind.combine(vaultState, textFillProperty(), this::getStatusIconColor));
		statusText.setMinSize(16.0, 16.0);
		statusText.setAlignment(Pos.CENTER);
		statusText.getStyleClass().add("fontawesome");

		tooltipProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::displayablePath).map(p -> new Tooltip(p.toString())));
		contextMenuProperty().bind(EasyBind.map(vaultState, this::getContextMenu));

		setGraphic(hbox);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	private String getStatusIconText(Vault.State state) {
		if (state == null) {
			return "";
		}
		switch (state) {
		case UNLOCKED:
		case PROCESSING:
			return "\uf09c";
		case LOCKED:
		default:
			return "\uf023";
		}
	}

	private Paint getStatusIconColor(Vault.State state, Paint lockedValue) {
		if (state == null) {
			return lockedValue;
		}
		switch (state) {
		case UNLOCKED:
		case PROCESSING:
			return UNLOCKED_ICON_COLOR;
		case LOCKED:
		default:
			return lockedValue;
		}
	}

	private ContextMenu getContextMenu(Vault.State state) {
		if (state == Vault.State.LOCKED) {
			return vaultContextMenu;
		} else {
			return null;
		}
	}

	public void setVaultContextMenu(ContextMenu contextMenu) {
		this.vaultContextMenu = contextMenu;
	}

}
