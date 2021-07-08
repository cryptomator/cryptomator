package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;

class CheckListCell extends ListCell<Check> {

	private final FontAwesome5IconView stateIcon = new FontAwesome5IconView();
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

		EasyBind.includeWhen(stateIcon.getStyleClass(), "glyph-icon-muted", stateIcon.glyphProperty().isEqualTo(FontAwesome5Icon.INFO_CIRCLE));
		EasyBind.includeWhen(stateIcon.getStyleClass(), "glyph-icon-primary", stateIcon.glyphProperty().isEqualTo(FontAwesome5Icon.CHECK));
		EasyBind.includeWhen(stateIcon.getStyleClass(), "glyph-icon-orange", stateIcon.glyphProperty().isEqualTo(FontAwesome5Icon.EXCLAMATION_TRIANGLE));
		EasyBind.includeWhen(stateIcon.getStyleClass(), "glyph-icon-red", stateIcon.glyphProperty().isEqualTo(FontAwesome5Icon.TIMES));
	}

	@Override
	protected void updateItem(Check item, boolean empty) {
		super.updateItem(item, empty);
		if (item != null) {
			setText(item.getLocalizedName());
			setGraphic(graphicContainer);
			checkBox.visibleProperty().bind(Bindings.createBooleanBinding(() -> item.getState() == Check.CheckState.RUNNABLE, item.stateProperty()));
			stateIcon.visibleProperty().bind(Bindings.createBooleanBinding(() -> item.getState() != Check.CheckState.RUNNABLE, item.stateProperty()));
			stateIcon.glyphProperty().bind(Bindings.createObjectBinding(() -> glyphForState(item), item.stateProperty(), item.highestResultSeverityProperty()));
			checkBox.selectedProperty().bindBidirectional(item.chosenForExecutionProperty());
		} else {
			graphicProperty();
			checkBox.visibleProperty().unbind();
			stateIcon.visibleProperty().unbind();
			setGraphic(null);
			setText(null);
			checkBox.selectedProperty().unbind();
		}
	}

	private FontAwesome5Icon glyphForState(Check item) {
		return switch (item.getState()) {
			case RUNNABLE -> null;
			case SKIPPED -> FontAwesome5Icon.FAST_FORWARD;
			case SCHEDULED -> FontAwesome5Icon.CLOCK;
			case RUNNING -> FontAwesome5Icon.SPINNER;
			case ERROR -> FontAwesome5Icon.TIMES;
			case CANCELLED -> FontAwesome5Icon.BAN;
			case SUCCEEDED -> {
				if (item.getHighestResultSeverity() == DiagnosticResult.Severity.INFO || item.getHighestResultSeverity() == DiagnosticResult.Severity.GOOD) {
					yield FontAwesome5Icon.CHECK;
				} else {
					yield FontAwesome5Icon.EXCLAMATION_TRIANGLE;
				}
			}
		};
	}

}
