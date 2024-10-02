package org.cryptomator.ui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class NotificationBar extends HBox {

	@FXML
	private Label notificationLabel;

	private final BooleanProperty dismissable = new SimpleBooleanProperty();
	private final BooleanProperty notify = new SimpleBooleanProperty();


	public NotificationBar() {
		setAlignment(Pos.CENTER);
		setStyle("-fx-alignment: center;");

		Region spacer = new Region();
		spacer.setMinWidth(40);

		Region leftRegion = new Region();
		HBox.setHgrow(leftRegion, javafx.scene.layout.Priority.ALWAYS);

		Region rightRegion = new Region();
		HBox.setHgrow(rightRegion, javafx.scene.layout.Priority.ALWAYS);

		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		HBox.setHgrow(vbox, javafx.scene.layout.Priority.ALWAYS);

		notificationLabel = new Label();
		notificationLabel.getStyleClass().add("notification-label");
		notificationLabel.setStyle("-fx-alignment: center;");
		vbox.getChildren().add(notificationLabel);

		Button closeButton = new Button("X");
		closeButton.setMinWidth(40);
		closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold;");
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
		return notificationLabel.getText();
	}

	public void setText(String text) {
		notificationLabel.setText(text);
	}

	public void setStyleClass(String styleClass) {
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
