package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import com.tobiasdiez.easybind.Subscription;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.StringConverter;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.cryptomator.cryptofs.health.api.DiagnosticResult.Severity;
import static org.cryptomator.ui.health.Result.FixState.FIXABLE;
import static org.cryptomator.ui.health.Result.FixState.FIXED;
import static org.cryptomator.ui.health.Result.FixState.FIXING;
import static org.cryptomator.ui.health.Result.FixState.FIX_FAILED;
import static org.cryptomator.ui.health.Result.FixState.NOT_FIXABLE;

@HealthCheckScoped
public class CheckDetailController implements FxController {

	private final EasyObservableList<Result> results;
	private final ObjectProperty<Check> check;
	private final ObservableValue<Check.CheckState> checkState;
	private final ObservableValue<String> checkName;
	private final BooleanExpression checkRunning;
	private final BooleanExpression checkScheduled;
	private final BooleanExpression checkFinished;
	private final BooleanExpression checkSkipped;
	private final BooleanExpression checkSucceeded;
	private final BooleanExpression checkFailed;
	private final BooleanExpression checkCancelled;
	private final Binding<Number> countOfWarnSeverity;
	private final Binding<Number> countOfCritSeverity;
	private final Binding<Boolean> warnOrCritsExist;
	private final ResultListCellFactory resultListCellFactory;
	private final ResultFixApplier resultFixApplier;
	private final ResourceBundle resourceBundle;

	private final BooleanProperty fixAllInfoResultsExecuted;
	private final BooleanBinding fixAllInfoResultsPossible;
	private final ObjectProperty<Predicate<Result>> resultsFilter;

	public ListView<Result> resultsListView;
	public ChoiceBox<DiagnosticResult.Severity> severityChoiceBox;
	public ChoiceBox<Result.FixState> fixStateChoiceBox;
	private Subscription resultSubscription;

	@Inject
	public CheckDetailController(ObjectProperty<Check> selectedTask, ResultListCellFactory resultListCellFactory, ResultFixApplier resultFixApplier, ResourceBundle resourceBundle) {
		this.resultListCellFactory = resultListCellFactory;
		this.resultFixApplier = resultFixApplier;
		this.resourceBundle = resourceBundle;
		this.results = EasyBind.wrapList(FXCollections.observableArrayList());
		this.check = selectedTask;
		this.checkState = selectedTask.flatMap(Check::stateProperty);
		this.checkName = selectedTask.map(Check::getName).orElse("");
		this.checkRunning = BooleanExpression.booleanExpression(checkState.map(Check.CheckState.RUNNING::equals).orElse(false));
		this.checkScheduled = BooleanExpression.booleanExpression(checkState.map(Check.CheckState.SCHEDULED::equals).orElse(false));
		this.checkSkipped = BooleanExpression.booleanExpression(checkState.map(Check.CheckState.SKIPPED::equals).orElse(false));
		this.checkSucceeded = BooleanExpression.booleanExpression(checkState.map(Check.CheckState.SUCCEEDED::equals).orElse(false));
		this.checkFailed = BooleanExpression.booleanExpression(checkState.map(Check.CheckState.ERROR::equals).orElse(false));
		this.checkCancelled = BooleanExpression.booleanExpression(checkState.map(Check.CheckState.CANCELLED::equals).orElse(false));
		this.checkFinished = checkSucceeded.or(checkFailed).or(checkCancelled);
		this.countOfWarnSeverity = results.reduce(countSeverity(Severity.WARN));
		this.countOfCritSeverity = results.reduce(countSeverity(Severity.CRITICAL));
		this.warnOrCritsExist = EasyBind.combine(checkSucceeded, countOfWarnSeverity, countOfCritSeverity, (suceeded, warns, crits) -> suceeded && (warns.longValue() > 0 || crits.longValue() > 0));
		this.fixAllInfoResultsExecuted = new SimpleBooleanProperty(false);
		this.fixAllInfoResultsPossible = Bindings.createBooleanBinding(() -> results.stream().anyMatch(this::isFixableInfoResult), results) //
				.and(fixAllInfoResultsExecuted.not());
		this.resultsFilter = new SimpleObjectProperty<>(r -> true);
		selectedTask.addListener(this::selectedTaskChanged);
	}

	private boolean isFixableInfoResult(Result r) {
		return r.diagnosis().getSeverity() == Severity.INFO && r.getState() == FIXABLE;
	}

	private void selectedTaskChanged(ObservableValue<? extends Check> observable, Check oldValue, Check newValue) {
		if (resultSubscription != null) {
			resultSubscription.unsubscribe();
		}
		if (newValue != null) {
			resultSubscription = EasyBind.bindContent(results, newValue.getResults());
		}
		severityChoiceBox.setValue(null);
		fixStateChoiceBox.setValue(null);
	}

	private Function<Stream<? extends Result>, Long> countSeverity(DiagnosticResult.Severity severity) {
		return stream -> stream.filter(item -> severity.equals(item.diagnosis().getSeverity())).count();
	}

	@FXML
	public void initialize() {
		resultsListView.setItems(results.filtered(resultsFilter));
		resultsListView.setCellFactory(resultListCellFactory);

		severityChoiceBox.getItems().add(null);
		severityChoiceBox.getItems().addAll(Arrays.stream(DiagnosticResult.Severity.values()).toList());
		severityChoiceBox.setConverter(new SeverityStringifier());
		severityChoiceBox.setValue(null);

		fixStateChoiceBox.getItems().add(null);
		fixStateChoiceBox.getItems().addAll(Arrays.stream(Result.FixState.values()).toList());
		fixStateChoiceBox.setConverter(new FixStateStringifier());
		fixStateChoiceBox.setValue(null);

		resultsFilter.bind(Bindings.createObjectBinding(() -> this::filterResults, severityChoiceBox.valueProperty(), fixStateChoiceBox.valueProperty()));
	}

	private boolean filterResults(Result r) {
		var desiredFixState = fixStateChoiceBox.getValue();
		var desiredSeverity = severityChoiceBox.getValue();
		return (desiredFixState == null || r.getState() == desiredFixState) && (desiredSeverity == null || r.diagnosis().getSeverity() == desiredSeverity);
	}

	@FXML
	public void fixAllInfoResults() {
		fixAllInfoResultsExecuted.setValue(true);
		results.stream().filter(this::isFixableInfoResult).forEach(resultFixApplier::fix);
	}


	@FXML
	public void copyResultDetails() {
		var result = resultsListView.getSelectionModel().getSelectedItem();
		if (result != null) {
			ClipboardContent clipboardContent = new ClipboardContent();
			clipboardContent.putString(result.diagnosis().toString());
			Clipboard.getSystemClipboard().setContent(clipboardContent);
		}
	}

	/* -- Internal classes -- */

	class SeverityStringifier extends StringConverter<Severity> {

		@Override
		public String toString(Severity object) {
			if (object == null) {
				return resourceBundle.getString("health.result.severityFilter.all");
			}
			return switch (object) {
				case GOOD -> resourceBundle.getString("health.result.severityFilter.good");
				case INFO -> resourceBundle.getString("health.result.severityFilter.info");
				case WARN -> resourceBundle.getString("health.result.severityFilter.warn");
				case CRITICAL -> resourceBundle.getString("health.result.severityFilter.crit");
			};
		}

		@Override
		public Severity fromString(String string) {
			if (resourceBundle.getString("health.result.severityFilter.good").equals(string)) {
				return Severity.GOOD;
			} else if (resourceBundle.getString("health.result.severityFilter.info").equals(string)) {
				return Severity.INFO;
			} else if (resourceBundle.getString("health.result.severityFilter.warn").equals(string)) {
				return Severity.WARN;
			} else if (resourceBundle.getString("health.result.severityFilter.crit").equals(string)) {
				return Severity.CRITICAL;
			} else {
				return null;
			}
		}
	}

	class FixStateStringifier extends StringConverter<Result.FixState> {

		@Override
		public String toString(Result.FixState object) {
			if (object == null) {
				return resourceBundle.getString("health.result.fixStateFilter.all");
			}
			return switch (object) {
				case FIXABLE -> resourceBundle.getString("health.result.fixStateFilter.fixable");
				case NOT_FIXABLE -> resourceBundle.getString("health.result.fixStateFilter.notFixable");
				case FIXING -> resourceBundle.getString("health.result.fixStateFilter.fixing");
				case FIXED -> resourceBundle.getString("health.result.fixStateFilter.fixed");
				case FIX_FAILED -> resourceBundle.getString("health.result.fixStateFilter.fixFailed");
			};
		}

		@Override
		public Result.FixState fromString(String string) {
			if (resourceBundle.getString("health.result.fixStateFilter.fixable").equals(string)) {
				return FIXABLE;
			} else if (resourceBundle.getString("health.result.fixStateFilter.notFixable").equals(string)) {
				return NOT_FIXABLE;
			} else if (resourceBundle.getString("health.result.fixStateFilter.fixing").equals(string)) {
				return FIXING;
			} else if (resourceBundle.getString("health.result.fixStateFilter.fixed").equals(string)) {
				return FIXED;
			} else if (resourceBundle.getString("health.result.fixStateFilter.fixFailed").equals(string)) {
				return FIX_FAILED;
			} else {
				return null;
			}
		}
	}

	/* Getter/Setter */

	public String getCheckName() {
		return checkName.getValue();
	}

	public ObservableValue<String> checkNameProperty() {
		return checkName;
	}

	public long getCountOfWarnSeverity() {
		return countOfWarnSeverity.getValue().longValue();
	}

	public Binding<Number> countOfWarnSeverityProperty() {
		return countOfWarnSeverity;
	}

	public long getCountOfCritSeverity() {
		return countOfCritSeverity.getValue().longValue();
	}

	public Binding<Number> countOfCritSeverityProperty() {
		return countOfCritSeverity;
	}

	public boolean isCheckRunning() {
		return checkRunning.getValue();
	}

	public BooleanExpression checkRunningProperty() {
		return checkRunning;
	}

	public boolean isCheckFinished() {
		return checkFinished.getValue();
	}

	public BooleanExpression checkFinishedProperty() {
		return checkFinished;
	}

	public boolean isCheckScheduled() {
		return checkScheduled.getValue();
	}

	public BooleanExpression checkScheduledProperty() {
		return checkScheduled;
	}

	public boolean isCheckSkipped() {
		return checkSkipped.getValue();
	}

	public BooleanExpression checkSkippedProperty() {
		return checkSkipped;
	}

	public boolean isCheckSucceeded() {
		return checkSucceeded.getValue();
	}

	public BooleanExpression checkSucceededProperty() {
		return checkSucceeded;
	}

	public boolean isCheckFailed() {
		return checkFailed.getValue();
	}

	public BooleanExpression checkFailedProperty() {
		return checkFailed;
	}

	public boolean isCheckCancelled() {
		return checkCancelled.getValue();
	}

	public Binding<Boolean> warnOrCritsExistProperty() {
		return warnOrCritsExist;
	}

	public boolean isWarnOrCritsExist() {
		return warnOrCritsExist.getValue();
	}

	public BooleanExpression checkCancelledProperty() {
		return checkCancelled;
	}

	public ObjectProperty<Check> checkProperty() {
		return check;
	}

	public Check getCheck() {
		return check.get();
	}

	public ObservableValue<Boolean> fixAllInfoResultsPossibleProperty() {
		return fixAllInfoResultsPossible;
	}

	public boolean getFixAllInfoResultsPossible() {
		return fixAllInfoResultsPossible.getValue();
	}
}
