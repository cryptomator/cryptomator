package org.cryptomator.ui.dialogs;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SimpleDialogController implements FxController {

	@FXML
	private Label messageLabel;
	@FXML
	private Label descriptionLabel;
	@FXML
	private FontAwesome5IconView iconView;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	private Runnable okAction;
	private Runnable cancelAction;

	public void setMessage(String message) {
		messageLabel.setText(message);
	}

	public void setDescription(String description) {
		descriptionLabel.setText(description);
	}

	public void setIcon(FontAwesome5Icon icon) {
		iconView.setGlyph(icon);
	}

	public void setOkButtonText(String text) {
		okButton.setText(text);
	}

	public void setCancelButtonText(String text) {
		cancelButton.setText(text);
	}

	public void setOkAction(Runnable action) {
		this.okAction = action;
	}

	public void setCancelAction(Runnable action) {
		this.cancelAction = action;
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