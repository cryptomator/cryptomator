package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

// unscoped because each cell needs its own controller
public class ResultListCellController implements FxController {

	private final Logger LOG = LoggerFactory.getLogger(ResultListCellController.class);

	private final ObjectProperty<Result> result;
	private final Binding<String> description;
	private final ResultFixApplier fixApplier;
	private final OptionalBinding<Result.FixState> fixState;
	private final ObjectBinding<FontAwesome5Icon> glyph;
	private final BooleanBinding fixable;
	private final BooleanBinding fixing;
	private final BooleanBinding fixed;

	public FontAwesome5IconView iconView;
	public Button fixButton;

	@Inject
	public ResultListCellController(ResultFixApplier fixApplier) {
		this.result = new SimpleObjectProperty<>(null);
		this.description = EasyBind.wrapNullable(result).map(Result::getDescription).orElse("");
		this.fixApplier = fixApplier;
		this.fixState = EasyBind.wrapNullable(result).mapObservable(Result::fixState);
		this.glyph = Bindings.createObjectBinding(this::getGlyph, result);
		this.fixable = Bindings.createBooleanBinding(this::isFixable, fixState);
		this.fixing = Bindings.createBooleanBinding(this::isFixing, fixState);
		this.fixed = Bindings.createBooleanBinding(this::isFixed, fixState);
	}

	@FXML
	public void fix() {
		final var realResult = result.get();
		if (realResult != null) {
			fixApplier.fix(realResult);
		}
	}

	@FXML
	public void initialize() {
		// see getGlyph() for relevant glyphs:
		EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-muted", iconView.glyphProperty().isEqualTo(FontAwesome5Icon.INFO_CIRCLE));
		EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-primary", iconView.glyphProperty().isEqualTo(FontAwesome5Icon.CHECK));
		EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-orange", iconView.glyphProperty().isEqualTo(FontAwesome5Icon.EXCLAMATION_TRIANGLE));
		EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-red", iconView.glyphProperty().isEqualTo(FontAwesome5Icon.TIMES));
	}

	/* Getter & Setter */

	public Result getResult() {
		return result.get();
	}

	public void setResult(Result result) {
		this.result.set(result);
	}

	public ObjectProperty<Result> resultProperty() {
		return result;
	}

	public String getDescription() {
		return description.getValue();
	}

	public ObjectBinding<FontAwesome5Icon> glyphProperty() {
		return glyph;
	}

	public FontAwesome5Icon getGlyph() {
		var r = result.get();
		if (r == null) {
			return null;
		}
		return switch (r.diagnosis().getSeverity()) {
			case INFO -> FontAwesome5Icon.INFO_CIRCLE;
			case GOOD -> FontAwesome5Icon.CHECK;
			case WARN -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
			case CRITICAL -> FontAwesome5Icon.TIMES;
		};
	}

	public Binding<String> descriptionProperty() {
		return description;
	}

	public BooleanBinding fixableProperty() {
		return fixable;
	}

	public boolean isFixable() {
		return fixState.get().map(Result.FixState.FIXABLE::equals).orElse(false);
	}

	public BooleanBinding fixingProperty() {
		return fixing;
	}

	public boolean isFixing() {
		return fixState.get().map(Result.FixState.FIXING::equals).orElse(false);
	}

	public BooleanBinding fixedProperty() {
		return fixed;
	}

	public boolean isFixed() {
		return fixState.get().map(Result.FixState.FIXED::equals).orElse(false);
	}

}
