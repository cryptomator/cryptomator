package org.cryptomator.ui.controls;

import javafx.scene.control.ListCell;

import org.cryptomator.ui.model.Directory;

public class DirectoryListCell extends ListCell<Directory> {

	@Override
	protected void updateItem(Directory item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null) {
			setText(null);
		} else {
			setText(item.getName());
		}
	}

}
