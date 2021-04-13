package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.HealthCheck;

import javafx.scene.control.ListCell;

class CheckListCell extends ListCell<HealthCheck> {

	@Override
	protected void updateItem(HealthCheck item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.identifier());
		}
	}
}
