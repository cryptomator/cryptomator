package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.FxmlFile;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import java.io.IOException;

public class NotificationBar extends HBox {

	@FXML
	private Label notificationLabel;

	@FXML
	private Button closeButton;

	private final BooleanProperty dismissable = new SimpleBooleanProperty();
	private final BooleanProperty notify = new SimpleBooleanProperty();


	public NotificationBar() {
		loadFXML();
		closeButton.visibleProperty().bind(dismissable);

		visibleProperty().bind(notifyProperty());
		managedProperty().bind(notifyProperty());

		closeButton.setOnAction(_ -> {
			visibleProperty().unbind();
			managedProperty().unbind();
			visibleProperty().set(false);
			managedProperty().set(false);
		});
	}

	private void loadFXML() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FxmlFile.NOTIFICATION.getRessourcePathString()));
		fxmlLoader.setController(this);
		try {
			HBox content = fxmlLoader.load();
			this.getChildren().addAll(content.getChildren());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
