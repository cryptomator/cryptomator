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
import javafx.scene.control.Button;

// unscoped because each cell needs its own controller
public class ResultListCellController implements FxController {

	private final ResultFixApplier fixApplier;
	private final ObjectProperty<DiagnosticResult> result;
	private final Binding<String> description;

	public FontAwesome5IconView iconView;
	public Button actionButton;

	@Inject
	public ResultListCellController(ResultFixApplier fixApplier) {
		this.result = new SimpleObjectProperty<>(null);
		this.description = EasyBind.wrapNullable(result).map(DiagnosticResult::toString).orElse("");
		this.fixApplier = fixApplier;
		result.addListener(this::updateCellContent);
	}

	private void updateCellContent(ObservableValue<? extends DiagnosticResult> observable, DiagnosticResult oldVal, DiagnosticResult newVal) {
		iconView.getStyleClass().clear();
		actionButton.setVisible(false);
		switch (newVal.getServerity()) {
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
				actionButton.setVisible(true);
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
			fixApplier.fix(realResult);
		}
	}
	/* Getter & Setter */


	public DiagnosticResult getResult() {
		return result.get();
	}

	public void setResult(DiagnosticResult result) {
		this.result.set(result);
	}

	public ObjectProperty<DiagnosticResult> resultProperty() {
		return result;
	}

	public String getDescription() {
		return description.getValue();
	}

	public Binding<String> descriptionProperty() {
		return description;
	}
}
