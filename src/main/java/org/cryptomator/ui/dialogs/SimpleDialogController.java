package org.cryptomator.ui.dialogs;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;

public class SimpleDialogController implements FxController {

	private final String message;
	private final String description;
	private final FontAwesome5Icon icon;
	private final String okButtonText;
	private final String cancelButtonText;
	private final Runnable okAction;
	private final Runnable cancelAction;
	private final BooleanProperty cancelButtonVisible = new SimpleBooleanProperty(true);

	public SimpleDialogController(String message, String description, FontAwesome5Icon icon, String okButtonText, String cancelButtonText, Runnable okAction, Runnable cancelAction) {
		this.message = message;
		this.description = description;
		this.icon = icon;
		this.okButtonText = okButtonText;
		this.cancelButtonText = cancelButtonText;
		this.okAction = okAction;
		this.cancelAction = cancelAction;
		this.cancelButtonVisible.set(cancelButtonText != null && !cancelButtonText.isEmpty());
	}

	public BooleanProperty cancelButtonVisibleProperty() {
		return cancelButtonVisible;
	}

	public boolean isCancelButtonVisible() {
		return cancelButtonVisible.get();
	}

	public String getMessage() {
		return message;
	}

	public String getDescription() {
		return description;
	}

	public FontAwesome5Icon getIcon() {
		return icon;
	}

	public String getOkButtonText() {
		return okButtonText;
	}

	public String getCancelButtonText() {
		return cancelButtonText;
	}

	@FXML
	private void handleOk() {
		if (okAction != null) {
			okAction.run();
		}
	}

	@FXML
	private void handleCancel() {
		if (cancelAction != null) {
			cancelAction.run();
		}
	}
}