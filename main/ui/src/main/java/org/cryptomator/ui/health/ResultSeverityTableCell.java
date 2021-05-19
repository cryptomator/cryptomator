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
			switch (item) {
				case INFO -> {
					iconView.setGlyph(FontAwesome5Icon.INFO_CIRCLE);
					iconView.getStyleClass().add("glyph-icon-muted");
				}
				case GOOD -> {
					iconView.setGlyph(FontAwesome5Icon.CHECK);
					iconView.getStyleClass().add("glyph-icon-primary");
				}
				case WARN -> {
					iconView.setGlyph(FontAwesome5Icon.EXCLAMATION_TRIANGLE);
					iconView.getStyleClass().add("glyph-icon-orange");
				}
				case CRITICAL -> {
					iconView.setGlyph(FontAwesome5Icon.TIMES);
					iconView.getStyleClass().add("glyph-icon-red");
				}
			}
			setGraphic(iconView);
			setText(item.name());
		}
	}
}
