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

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class DirectoryListCell extends DraggableListCell<Vault> {

	// fill: #FD4943, stroke: #E1443F
	private static final Color RED_FILL = Color.rgb(253, 73, 67);
	private static final Color RED_STROKE = Color.rgb(225, 68, 63);

	// fill: #FFBF2F, stroke: #E4AC36
	// private static final Color YELLOW_FILL = Color.rgb(255, 191, 47);
	// private static final Color YELLOW_STROKE = Color.rgb(228, 172, 54);

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

		nameText.textProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::name));
		nameText.textFillProperty().bind(this.textFillProperty());
		nameText.fontProperty().bind(this.fontProperty());

		pathText.textProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::displayablePath));
		pathText.setTextOverrun(OverrunStyle.ELLIPSIS);
		pathText.getStyleClass().add("detail-label");

		statusIndicator.fillProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::unlockedProperty).filter(Boolean.TRUE::equals).map(unlocked -> GREEN_FILL).orElse(RED_FILL));
		statusIndicator.strokeProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::unlockedProperty).filter(Boolean.TRUE::equals).map(unlocked -> GREEN_STROKE).orElse(RED_STROKE));

		tooltipProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::path).map(p -> new Tooltip(p.toString())));
		contextMenuProperty().bind(EasyBind.monadic(itemProperty()).flatMap(Vault::unlockedProperty).map(unlocked -> {
			return unlocked ? null : vaultContextMenu;
		}));

		setGraphic(hbox);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	@Override
	protected void updateItem(Vault item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null) {
			statusIndicator.setVisible(false);
		} else {
			statusIndicator.setVisible(true);
		}
	}

	public void setVaultContextMenu(ContextMenu contextMenu) {
		this.vaultContextMenu = contextMenu;
	}

}
