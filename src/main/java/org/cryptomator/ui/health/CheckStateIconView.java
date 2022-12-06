package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import java.util.List;

/**
 * A {@link FontAwesome5IconView} that automatically sets the glyph depending on
 * the {@link Check#stateProperty() state} and {@link Check#highestResultSeverityProperty() severity} of a HealthCheck.
 */
public class CheckStateIconView extends FontAwesome5IconView {

	private final ObjectProperty<Check> check = new SimpleObjectProperty<>();
	private final ObservableObjectValue<Check.CheckState> state;
	private final ObservableObjectValue<DiagnosticResult.Severity> severity;
	private final List<Subscription> subscriptions;
	private final AutoAnimator onRunningRotator;

	public CheckStateIconView() {
		this.state = EasyBind.wrapNullable(check).mapObservable(Check::stateProperty).asOrdinary();
		this.severity = EasyBind.wrapNullable(check).mapObservable(Check::highestResultSeverityProperty).asOrdinary();
		this.glyph.bind(Bindings.createObjectBinding(this::glyphForState, state, severity));
		this.subscriptions = List.of( //
				EasyBind.includeWhen(getStyleClass(), "glyph-icon-muted", Bindings.equal(state, Check.CheckState.SKIPPED).or(Bindings.equal(state, Check.CheckState.CANCELLED)).or(Bindings.equal(severity, DiagnosticResult.Severity.INFO))), //
				EasyBind.includeWhen(getStyleClass(), "glyph-icon-primary", Bindings.equal(severity, DiagnosticResult.Severity.GOOD)), //
				EasyBind.includeWhen(getStyleClass(), "glyph-icon-orange", Bindings.equal(severity, DiagnosticResult.Severity.WARN).or(Bindings.equal(severity, DiagnosticResult.Severity.CRITICAL))), //
				EasyBind.includeWhen(getStyleClass(), "glyph-icon-red", Bindings.equal(state, Check.CheckState.ERROR)) //
		);
		var animation = Animations.createDiscrete360Rotation(this);
		this.onRunningRotator = AutoAnimator.animate(animation) //
				.onCondition(Bindings.equal(state, Check.CheckState.RUNNING)) //
				.afterStop(() -> setRotate(0)) //
				.build();
	}

	private FontAwesome5Icon glyphForState() {
		if (state.getValue() == null) {
			return null;
		}
		return switch (state.getValue()) {
			case RUNNABLE -> null;
			case SKIPPED -> FontAwesome5Icon.FAST_FORWARD;
			case SCHEDULED -> FontAwesome5Icon.CLOCK;
			case RUNNING -> FontAwesome5Icon.SPINNER;
			case ERROR -> FontAwesome5Icon.TIMES;
			case CANCELLED -> FontAwesome5Icon.BAN;
			case SUCCEEDED -> glyphIconForSeverity();
		};
	}

	private FontAwesome5Icon glyphIconForSeverity() {
		if (severity.getValue() == null) {
			return null;
		}
		return switch (severity.getValue()) {
			case GOOD, INFO -> FontAwesome5Icon.CHECK;
			case WARN, CRITICAL -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
		};
	}

	public ObjectProperty<Check> checkProperty() {
		return check;
	}

	public void setCheck(Check c) {
		check.set(c);
	}

	public Check getCheck() {
		return check.get();
	}

}
