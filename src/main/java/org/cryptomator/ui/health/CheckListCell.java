package org.cryptomator.ui.health;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;

class CheckListCell extends ListCell<Check> {

	private final CheckStateIconView stateIcon = new CheckStateIconView();
	private CheckBox checkBox = new CheckBox();
	private final StackPane graphicContainer = new StackPane(stateIcon, checkBox);

	CheckListCell() {
		setPadding(new Insets(6));
		setAlignment(Pos.CENTER_LEFT);
		setContentDisplay(ContentDisplay.LEFT);
		getStyleClass().add("label");
		graphicContainer.minWidth(20);
		graphicContainer.maxWidth(20);
		graphicContainer.setAlignment(Pos.CENTER);
	}

	@Override
	protected void updateItem(Check item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.getLocalizedName());
			setGraphic(graphicContainer);
			stateIcon.setCheck(item);
			checkBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> item.getState() == Check.CheckState.RUNNABLE, item.stateProperty()));
			stateIcon.visibleProperty().bind(Bindings.createBooleanBinding(() -> item.getState() != Check.CheckState.RUNNABLE, item.stateProperty()));
			checkBox.selectedProperty().bindBidirectional(item.chosenForExecutionProperty());
		} else {
			graphicProperty();
			checkBox.visibleProperty().unbind();
			stateIcon.visibleProperty().unbind();
			stateIcon.setCheck(null);
			setGraphic(null);
			setText(null);
			checkBox.selectedProperty().unbind();
		}
	}

}
