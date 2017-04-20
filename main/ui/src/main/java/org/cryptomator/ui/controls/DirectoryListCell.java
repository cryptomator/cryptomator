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
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DirectoryListCell extends DraggableListCell<Vault> {

	private final Label statusText = new Label();
	private final Label nameText = new Label();
	private final Label pathText = new Label();
	private final VBox vbox = new VBox(4.0, nameText, pathText);
	private final HBox hbox = new HBox(6.0, statusText, vbox);
	private ContextMenu vaultContextMenu;

	public DirectoryListCell() {
		hbox.setAlignment(Pos.CENTER_LEFT);
		hbox.setPrefWidth(1);
		vbox.setFillWidth(true);

		nameText.textProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::name));
		nameText.textFillProperty().bind(this.textFillProperty());
		nameText.fontProperty().bind(this.fontProperty());

		pathText.textProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::displayablePath));
		pathText.setTextOverrun(OverrunStyle.ELLIPSIS);
		pathText.getStyleClass().add("detail-label");

		MonadicBinding<Boolean> optionalItemIsUnlocked = EasyBind.monadic(itemProperty()).flatMap(Vault::unlockedProperty);
		statusText.textProperty().bind(optionalItemIsUnlocked.map(this::getStatusIconText));
		statusText.textFillProperty().bind(this.textFillProperty());
		statusText.setMinSize(16.0, 16.0);
		statusText.setAlignment(Pos.CENTER);
		statusText.getStyleClass().add("fontawesome");

		tooltipProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::displayablePath).map(p -> new Tooltip(p.toString())));
		contextMenuProperty().bind(optionalItemIsUnlocked.map(unlocked -> unlocked ? null : vaultContextMenu));

		setGraphic(hbox);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	private String getStatusIconText(Boolean unlockedOrNull) {
		if (Boolean.TRUE.equals(unlockedOrNull)) {
			return "\uf09c";
		} else if (Boolean.FALSE.equals(unlockedOrNull)) {
			return "\uf023";
		} else {
			return "";
		}
	}

	public void setVaultContextMenu(ContextMenu contextMenu) {
		this.vaultContextMenu = contextMenu;
	}

}
