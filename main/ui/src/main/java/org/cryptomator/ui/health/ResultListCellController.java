package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

public class ResultListCellController implements FxController {

	private final ObjectProperty<DiagnosticResultAction> result;
	private final Binding<Boolean> fixButtonVisible;
	private final Binding<String> description;

	@FXML
	public FontAwesome5IconView iconView;

	@Inject
	public ResultListCellController() {
		this.result = new SimpleObjectProperty<>(null);
		this.fixButtonVisible = EasyBind.wrapNullable(result) //
				.map(val -> val.getSeverity() == DiagnosticResult.Severity.WARN).orElse(true); //
		this.description = EasyBind.wrapNullable(result).map(DiagnosticResultAction::getDescription).orElse("");
		result.addListener(this::getGlyphForSeverity);
	}

	private void getGlyphForSeverity(ObservableValue<? extends DiagnosticResultAction> observable, DiagnosticResultAction oldVal, DiagnosticResultAction newVal) {
		iconView.getStyleClass().clear();
		switch (newVal.getSeverity()) {
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
	}

	@FXML
	public void runResultAction() {
		final var realResult = result.get();
		if (realResult != null) {
			realResult.run(); //TODO: this hogs currently the JAVAFX thread
		}
	}

	/* Getter & Setter */

	public DiagnosticResultAction getResult() {
		return result.get();
	}

	public void setResult(DiagnosticResultAction result) {
		this.result.set(result);
	}

	public ObjectProperty<DiagnosticResultAction> resultProperty() {
		return result;
	}

	public boolean isFixButtonVisibile() {
		return fixButtonVisible.getValue();
	}

	public Binding<Boolean> fixButtonVisibleProperty() {
		return fixButtonVisible;
	}

	public String getDescription() {
		return description.getValue();
	}

	public Binding<String> descriptionProperty() {
		return description;
	}
}
