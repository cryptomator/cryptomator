package org.cryptomator.ui.health;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

class ResultActionTableCell extends TableCell<DiagnosticResultAction, Runnable> {

	private Button button;

	ResultActionTableCell() {
		button = new Button();
		setGraphic(null);
	}

	@Override
	protected void updateItem(Runnable item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			setGraphic(button);
			button.setOnAction(event -> item.run());
			button.setText("FIXME"); //FIXME
			button.setAlignment(Pos.CENTER);
		}
	}

}
