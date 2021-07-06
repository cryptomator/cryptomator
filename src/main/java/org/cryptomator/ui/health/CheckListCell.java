package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import java.util.function.Predicate;

class CheckListCell extends ListCell<HealthCheckTask> {

	private final FontAwesome5IconView stateIcon = new FontAwesome5IconView();
	private CheckBox checkBox = new CheckBox();

	CheckListCell() {
		setPadding(new Insets(6));
		setAlignment(Pos.CENTER_LEFT);
		setContentDisplay(ContentDisplay.LEFT);
		getStyleClass().add("label");
	}

	@Override
	protected void updateItem(HealthCheckTask item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.getTitle());
			graphicProperty().bind(Bindings.createObjectBinding(() -> graphicForState(item.getState()), item.stateProperty()));
			stateIcon.glyphProperty().bind(Bindings.createObjectBinding(() -> glyphForState(item), item.stateProperty()));
			checkBox.selectedProperty().bindBidirectional(item.chosenForExecutionProperty());
		} else {
			graphicProperty().unbind();
			setGraphic(null);
			setText(null);
			checkBox.selectedProperty().unbind();
		}
	}

	private Node graphicForState(Worker.State state) {
		return switch (state) {
			case READY -> checkBox;
			case SCHEDULED, RUNNING, FAILED, CANCELLED, SUCCEEDED -> stateIcon;
		};
	}

	private FontAwesome5Icon glyphForState(HealthCheckTask item) {
		return switch (item.getState()) {
			case READY -> FontAwesome5Icon.COG; //just a placeholder
			case SCHEDULED -> FontAwesome5Icon.CLOCK;
			case RUNNING -> FontAwesome5Icon.SPINNER;
			case FAILED -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
			case CANCELLED -> FontAwesome5Icon.BAN;
			case SUCCEEDED -> checkFoundProblems(item) ? FontAwesome5Icon.EXCLAMATION_TRIANGLE : FontAwesome5Icon.CHECK;
		};
	}

	private boolean checkFoundProblems(HealthCheckTask item) {
		Predicate<DiagnosticResult.Severity> isProblem = severity -> switch (severity) {
			case WARN, CRITICAL -> true;
			case INFO, GOOD -> false;
		};
		return item.results().stream().map(Result::diagnosis).map(DiagnosticResult::getSeverity).anyMatch(isProblem);
	}

}
