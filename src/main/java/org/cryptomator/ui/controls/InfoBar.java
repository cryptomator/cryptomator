package org.cryptomator.ui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.ResourceBundle;

public class InfoBar extends HBox {

	@FXML
	private Label infoMessage;

	private final BooleanProperty dismissable = new SimpleBooleanProperty();
	private final BooleanProperty notify = new SimpleBooleanProperty();


	public InfoBar() {
		setAlignment(Pos.CENTER);
		getStyleClass().addAll("info-bar");

		Region spacer = new Region();
		spacer.setMinWidth(40);

		Region leftRegion = new Region();
		HBox.setHgrow(leftRegion, javafx.scene.layout.Priority.ALWAYS);

		Region rightRegion = new Region();
		HBox.setHgrow(rightRegion, javafx.scene.layout.Priority.ALWAYS);

		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		HBox.setHgrow(vbox, javafx.scene.layout.Priority.ALWAYS);

		infoMessage = new Label();
		infoMessage.setFocusTraversable(true);
		infoMessage.setAccessibleRole(AccessibleRole.BUTTON);
		vbox.getChildren().add(infoMessage);

		var closeGraphic = new FontAwesome5IconView();
		closeGraphic.setGlyph(FontAwesome5Icon.TIMES);
		closeGraphic.setGlyphSize(12);
		closeGraphic.getStyleClass().add("glyph");

		Button closeButton = new Button();
		closeButton.setGraphic(closeGraphic);
		closeButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		closeButton.setAccessibleText(ResourceBundle.getBundle("i18n.strings").getString("main.notification.closeButton.tooltip"));
		closeButton.setMinWidth(40);
		closeButton.visibleProperty().bind(dismissable);

		closeButton.setOnAction(_ -> {
			visibleProperty().unbind();
			managedProperty().unbind();
			visibleProperty().set(false);
			managedProperty().set(false);
		});
		closeButton.visibleProperty().bind(dismissable);

		getChildren().addAll(spacer, leftRegion, vbox, rightRegion, closeButton);

		visibleProperty().bind(notifyProperty());
		managedProperty().bind(notifyProperty());
	}

	public String getText() {
		return infoMessage.getText();
	}

	public void setText(String text) {
		infoMessage.setText(text);
	}

	public void setStyleClass(String styleClass) {
		getStyleClass().clear();
		getStyleClass().add(styleClass);
	}

	public boolean isDismissable() {
		return dismissable.get();
	}

	public void setDismissable(boolean value) {
		dismissable.set(value);
	}

	public boolean getNotify() {
		return notify.get();
	}

	public void setNotify(boolean value) {
		notify.set(value);
	}

	public BooleanProperty notifyProperty() {
		return notify;
	}

}
