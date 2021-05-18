package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.scene.control.TableCell;

public class ResultSeverityTableCell extends TableCell<DiagnosticResultAction, DiagnosticResult.Severity> {

	private FontAwesome5IconView iconView;

	ResultSeverityTableCell() {
		iconView = new FontAwesome5IconView();
	}

	@Override
	protected void updateItem(DiagnosticResult.Severity item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			iconView.glyphProperty().set(switch (item) {
				case INFO -> FontAwesome5Icon.INFO_CIRCLE;
				case GOOD -> FontAwesome5Icon.CHECK;
				case WARN -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
				case CRITICAL -> FontAwesome5Icon.TIMES;
			});
			setGraphic(iconView);
			setText(item.name());
		}
	}
}
