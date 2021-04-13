package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;

import javafx.scene.control.ListCell;

class ResultListCell extends ListCell<DiagnosticResult> {

	@Override
	protected void updateItem(DiagnosticResult item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.toString());
		}
	}
}
