package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.List;
import java.util.Optional;

public class CheckStateIconView extends FontAwesome5IconView {

	private final ObjectProperty<Check> check = new SimpleObjectProperty<>();
	private final OptionalBinding<Check.CheckState> state;
	private final OptionalBinding<DiagnosticResult.Severity> severity;
	private List<Subscription> subscriptions;

	public CheckStateIconView() {
		super();
		this.getStyleClass().remove("glyph-icon");
		this.state = EasyBind.wrapNullable(check).mapObservable(Check::stateProperty);
		this.severity = EasyBind.wrapNullable(check).mapObservable(Check::highestResultSeverityProperty);
		glyphProperty().bind(EasyBind.combine(state, severity, this::glyphForState)); //TODO: does the binding need to be stored?
		this.subscriptions = List.of(
			EasyBind.includeWhen(getStyleClass(), "glyph-icon-muted", glyphProperty().isEqualTo(FontAwesome5Icon.INFO_CIRCLE)),
			EasyBind.includeWhen(getStyleClass(), "glyph-icon-primary", glyphProperty().isEqualTo(FontAwesome5Icon.CHECK)),
			EasyBind.includeWhen(getStyleClass(), "glyph-icon-orange", glyphProperty().isEqualTo(FontAwesome5Icon.EXCLAMATION_TRIANGLE)),
			EasyBind.includeWhen(getStyleClass(), "glyph-icon-red", glyphProperty().isEqualTo(FontAwesome5Icon.TIMES))
		);
	}

	private FontAwesome5Icon glyphForState(Optional<Check.CheckState> state, Optional<DiagnosticResult.Severity> severity) {
		return state.map(s -> switch (s) {
			case RUNNABLE -> null;
			case SKIPPED -> FontAwesome5Icon.FAST_FORWARD;
			case SCHEDULED -> FontAwesome5Icon.CLOCK;
			case RUNNING -> FontAwesome5Icon.SPINNER;
			case ERROR -> FontAwesome5Icon.TIMES;
			case CANCELLED -> FontAwesome5Icon.BAN;
			case SUCCEEDED -> severity.map(se -> DiagnosticResult.Severity.GOOD.compareTo(se) >= 0 ? FontAwesome5Icon.CHECK : FontAwesome5Icon.EXCLAMATION_TRIANGLE).orElse(null);
		}).orElse(null);
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
