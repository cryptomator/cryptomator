package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import com.tobiasdiez.easybind.Subscription;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.util.function.Function;
import java.util.stream.Stream;

@HealthCheckScoped
public class CheckDetailController implements FxController {

	private final EasyObservableList<Result> results;
	private final ObjectProperty<Check> check;
	private final OptionalBinding<Check.CheckState> checkState;
	private final Binding<String> checkName;
	private final Binding<Boolean> checkRunning;
	private final Binding<Boolean> checkScheduled;
	private final Binding<Boolean> checkFinished;
	private final Binding<Boolean> checkSkipped;
	private final Binding<Boolean> checkSucceeded;
	private final Binding<Boolean> checkFailed;
	private final Binding<Boolean> checkCancelled;
	private final Binding<Number> countOfWarnSeverity;
	private final Binding<Number> countOfCritSeverity;
	private final Binding<Boolean> warnOrCritsExist;
	private final ResultListCellFactory resultListCellFactory;

	public ListView<Result> resultsListView;
	private Subscription resultSubscription;

	@Inject
	public CheckDetailController(ObjectProperty<Check> selectedTask, ResultListCellFactory resultListCellFactory) {
		this.resultListCellFactory = resultListCellFactory;
		this.results = EasyBind.wrapList(FXCollections.observableArrayList());
		this.check = selectedTask;
		this.checkState = EasyBind.wrapNullable(selectedTask).mapObservable(Check::stateProperty);
		this.checkName = EasyBind.wrapNullable(selectedTask).map(Check::getName).orElse("");
		this.checkRunning = checkState.map(Check.CheckState.RUNNING::equals).orElse(false);
		this.checkScheduled = checkState.map(Check.CheckState.SCHEDULED::equals).orElse(false);
		this.checkSkipped = checkState.map(Check.CheckState.SKIPPED::equals).orElse(false);
		this.checkSucceeded = checkState.map(Check.CheckState.SUCCEEDED::equals).orElse(false);
		this.checkFailed = checkState.map(Check.CheckState.ERROR::equals).orElse(false);
		this.checkCancelled = checkState.map(Check.CheckState.CANCELLED::equals).orElse(false);
		this.checkFinished = EasyBind.combine(checkSucceeded, checkFailed, checkCancelled, (a, b, c) -> a || b || c);
		this.countOfWarnSeverity = results.reduce(countSeverity(DiagnosticResult.Severity.WARN));
		this.countOfCritSeverity = results.reduce(countSeverity(DiagnosticResult.Severity.CRITICAL));
		this.warnOrCritsExist = EasyBind.combine(checkSucceeded, countOfWarnSeverity, countOfCritSeverity, (suceeded, warns, crits) -> suceeded && (warns.longValue() > 0 || crits.longValue() > 0) );
		selectedTask.addListener(this::selectedTaskChanged);
	}

	private void selectedTaskChanged(ObservableValue<? extends Check> observable, Check oldValue, Check newValue) {
		if (resultSubscription != null) {
			resultSubscription.unsubscribe();
		}
		if (newValue != null) {
			resultSubscription = EasyBind.bindContent(results, newValue.getResults());
		}
	}

	private Function<Stream<? extends Result>, Long> countSeverity(DiagnosticResult.Severity severity) {
		return stream -> stream.filter(item -> severity.equals(item.diagnosis().getSeverity())).count();
	}

	@FXML
	public void initialize() {
		resultsListView.setItems(results);
		resultsListView.setCellFactory(resultListCellFactory);
	}

	/* Getter/Setter */

	public String getCheckName() {
		return checkName.getValue();
	}

	public Binding<String> checkNameProperty() {
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

	public Binding<Boolean> checkRunningProperty() {
		return checkRunning;
	}

	public boolean isCheckFinished() {
		return checkFinished.getValue();
	}

	public Binding<Boolean> checkFinishedProperty() {
		return checkFinished;
	}

	public boolean isCheckScheduled() {
		return checkScheduled.getValue();
	}

	public Binding<Boolean> checkScheduledProperty() {
		return checkScheduled;
	}

	public boolean isCheckSkipped() {
		return checkSkipped.getValue();
	}

	public Binding<Boolean> checkSkippedProperty() {
		return checkSkipped;
	}

	public boolean isCheckSucceeded() {
		return checkSucceeded.getValue();
	}

	public Binding<Boolean> checkSucceededProperty() {
		return checkSucceeded;
	}

	public boolean isCheckFailed() {
		return checkFailed.getValue();
	}

	public Binding<Boolean> checkFailedProperty() {
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

	public Binding<Boolean> checkCancelledProperty() {
		return checkCancelled;
	}

	public ObjectProperty<Check> checkProperty() {
		return check;
	}

	public Check getCheck() {
		return check.get();
	}
}
