package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.FxController;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CustomDialogController implements FxController {

	@FXML
	private Label message;
	@FXML
	private Label description;
	@FXML
	private FontAwesome5IconView icon;
	@FXML
	private Button okButton;

	private Stage dialogStage;
	private Runnable okAction;

	public void setDialogStage(Stage stage) {
		this.dialogStage = stage;
	}

	public void setIcon(FontAwesome5Icon glyph) {
		icon.setGlyph(glyph);
	}

	public void setMessage(String message) {
		this.message.setText(message);
	}

	public void setDescription(String desc) {
		this.description.setText(desc);
	}

	public void setOkAction(Runnable action) {
		this.okAction = action;
	}

	public void setOkButtonText(String text) {
		okButton.setText(text);
	}

	@FXML
	private void handleOk() {
		if (okAction != null) {
			okAction.run();
		}
		dialogStage.close();
	}

	@FXML
	private void handleCancel() {
		dialogStage.close();
	}

}
