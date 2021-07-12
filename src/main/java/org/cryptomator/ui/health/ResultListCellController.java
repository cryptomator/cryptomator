package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.ArrayList;
import java.util.List;

// unscoped because each cell needs its own controller
public class ResultListCellController implements FxController {

	//TODO: use different glyphs!
	private static final FontAwesome5Icon INFO_ICON = FontAwesome5Icon.INFO_CIRCLE;
	private static final FontAwesome5Icon GOOD_ICON = FontAwesome5Icon.CHECK;
	private static final FontAwesome5Icon WARN_ICON = FontAwesome5Icon.EXCLAMATION_TRIANGLE;
	private static final FontAwesome5Icon CRIT_ICON = FontAwesome5Icon.TIMES;

	private final Logger LOG = LoggerFactory.getLogger(ResultListCellController.class);

	private final ObjectProperty<Result> result;
	private final Binding<String> description;
	private final ResultFixApplier fixApplier;
	private final OptionalBinding<Result.FixState> fixState;
	private final ObjectBinding<FontAwesome5Icon> glyph;
	private final BooleanBinding fixable;
	private final BooleanBinding fixing;
	private final BooleanBinding fixed;
	private final List<Subscription> subscriptions;

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
		this.subscriptions = new ArrayList<>();
	}

	@FXML
	public void initialize() {
		// see getGlyph() for relevant glyphs:
		iconView.getStyleClass().remove("glyph-icon");
		subscriptions.addAll(List.of(
				EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-muted", iconView.glyphProperty().isEqualTo(INFO_ICON)), //
				EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-primary", iconView.glyphProperty().isEqualTo(GOOD_ICON)), //
				EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-orange", iconView.glyphProperty().isEqualTo(WARN_ICON)), //
				EasyBind.includeWhen(iconView.getStyleClass(), "glyph-icon-red", iconView.glyphProperty().isEqualTo(CRIT_ICON))) //
		);
	}

	@FXML
	public void fix() {
		Result r = result.get();
		if (r != null) {
			fixApplier.fix(r).whenCompleteAsync(this::fixFinished, Platform::runLater);
		}
	}

	private void fixFinished(Void unused, Throwable exception) {
		if (exception != null) {
			LOG.error("Failed to apply fix", exception);
			// TODO ...
		}
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
			case INFO -> INFO_ICON;
			case GOOD -> GOOD_ICON;
			case WARN -> WARN_ICON;
			case CRITICAL -> CRIT_ICON;
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
