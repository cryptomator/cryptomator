package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

// unscoped because each cell needs its own controller
public class ResultListCellController implements FxController {

	private static final FontAwesome5Icon INFO_ICON = FontAwesome5Icon.INFO_CIRCLE;
	private static final FontAwesome5Icon GOOD_ICON = FontAwesome5Icon.CHECK;
	private static final FontAwesome5Icon WARN_ICON = FontAwesome5Icon.EXCLAMATION_TRIANGLE;
	private static final FontAwesome5Icon CRIT_ICON = FontAwesome5Icon.TIMES;

	private final Logger LOG = LoggerFactory.getLogger(ResultListCellController.class);

	private final ObjectProperty<Result> result;
	private final ObservableValue<DiagnosticResult.Severity> severity;
	private final ObservableValue<String> description;
	private final ResultFixApplier fixApplier;
	private final ObservableValue<Result.FixState> fixState;
	private final ObjectBinding<FontAwesome5Icon> severityGlyph;
	private final ObjectBinding<FontAwesome5Icon> fixGlyph;
	private final BooleanBinding fixable;
	private final BooleanBinding fixing;
	private final BooleanBinding fixed;
	private final BooleanBinding fixFailed;
	private final BooleanBinding fixRunningOrDone;
	private final List<Subscription> subscriptions;
	private final Tooltip fixSuccessTip;
	private final Tooltip fixFailTip;

	private AutoAnimator fixRunningRotator;

	/* FXML */
	public FontAwesome5IconView severityView;
	public FontAwesome5IconView fixView;

	@Inject
	public ResultListCellController(ResultFixApplier fixApplier, ResourceBundle resourceBundle) {
		this.result = new SimpleObjectProperty<>(null);
		this.severity = result.map(Result::diagnosis).map(DiagnosticResult::getSeverity);
		this.description = result.map(Result::getDescription).orElse("");
		this.fixApplier = fixApplier;
		this.fixState = result.flatMap(Result::fixState);
		this.severityGlyph = Bindings.createObjectBinding(this::getSeverityGlyph, result);
		this.fixGlyph = Bindings.createObjectBinding(this::getFixGlyph, fixState);
		this.fixable = Bindings.createBooleanBinding(this::isFixable, fixState);
		this.fixing = Bindings.createBooleanBinding(this::isFixing, fixState);
		this.fixed = Bindings.createBooleanBinding(this::isFixed, fixState);
		this.fixFailed = Bindings.createBooleanBinding(this::isFixFailed, fixState);
		this.fixRunningOrDone = fixing.or(fixed).or(fixFailed);
		this.subscriptions = new ArrayList<>();
		this.fixSuccessTip = new Tooltip(resourceBundle.getString("health.fix.successTip"));
		this.fixFailTip = new Tooltip(resourceBundle.getString("health.fix.failTip"));
		fixSuccessTip.setShowDelay(Duration.millis(100));
		fixFailTip.setShowDelay(Duration.millis(100));
	}

	@FXML
	public void initialize() {
		// see getGlyph() for relevant glyphs:
		subscriptions.addAll(List.of(EasyBind.includeWhen(severityView.getStyleClass(), "glyph-icon-muted", severity.map(DiagnosticResult.Severity.INFO::equals).orElse(false)), //
				EasyBind.includeWhen(severityView.getStyleClass(), "glyph-icon-primary", severity.map(DiagnosticResult.Severity.GOOD::equals).orElse(false)), //
				EasyBind.includeWhen(severityView.getStyleClass(), "glyph-icon-orange", severity.map(DiagnosticResult.Severity.WARN::equals).orElse(false)), //
				EasyBind.includeWhen(severityView.getStyleClass(), "glyph-icon-red", severity.map(DiagnosticResult.Severity.CRITICAL::equals).orElse(false)) //
		));

		var animation = Animations.createDiscrete360Rotation(fixView);
		this.fixRunningRotator = AutoAnimator.animate(animation) //
				.onCondition(fixing) //
				.afterStop(() -> fixView.setRotate(0)) //
				.build();
		fixState.addListener(((observable, oldValue, newValue) -> {
			if (newValue == Result.FixState.FIXED) {
				Tooltip.install(fixView, fixSuccessTip);
			} else if (newValue == Result.FixState.FIX_FAILED) {
				Tooltip.install(fixView, fixFailTip);
			}
		}));
	}

	@FXML
	public void fix() {
		Result r = result.get();
		if (r != null) {
			fixApplier.fix(r);
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

	public ObservableValue<String> descriptionProperty() {
		return description;
	}

	public String getDescription() {
		return description.getValue();
	}

	public ObjectBinding<FontAwesome5Icon> severityGlyphProperty() {
		return severityGlyph;
	}

	public FontAwesome5Icon getSeverityGlyph() {
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

	public ObjectBinding<FontAwesome5Icon> fixGlyphProperty() {
		return fixGlyph;
	}

	public FontAwesome5Icon getFixGlyph() {
		if (fixState.getValue() == null) {
			return null;
		}
		return switch (fixState.getValue()) {
			case NOT_FIXABLE, FIXABLE -> null;
			case FIXING -> FontAwesome5Icon.SPINNER;
			case FIXED -> FontAwesome5Icon.CHECK;
			case FIX_FAILED -> FontAwesome5Icon.TIMES;
		};
	}

	public BooleanBinding fixableProperty() {
		return fixable;
	}

	public boolean isFixable() {
		return Result.FixState.FIXABLE.equals(fixState.getValue());
	}

	public BooleanBinding fixingProperty() {
		return fixing;
	}

	public boolean isFixing() {
		return Result.FixState.FIXING.equals(fixState.getValue());
	}

	public BooleanBinding fixedProperty() {
		return fixed;
	}

	public boolean isFixed() {
		return Result.FixState.FIXED.equals(fixState.getValue());
	}

	public BooleanBinding fixFailedProperty() {
		return fixFailed;
	}

	public Boolean isFixFailed() {
		return Result.FixState.FIX_FAILED.equals(fixState.getValue());
	}

	public BooleanBinding fixRunningOrDoneProperty() {
		return fixRunningOrDone;
	}

	public boolean isFixRunningOrDone() {
		return fixRunningOrDone.get();
	}


}
