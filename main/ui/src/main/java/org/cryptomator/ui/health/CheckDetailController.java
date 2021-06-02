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
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.util.function.Function;
import java.util.stream.Stream;

@HealthCheckScoped
public class CheckDetailController implements FxController {

	private final EasyObservableList<DiagnosticResult> results;
	private final OptionalBinding<Worker.State> taskState;
	private final Binding<String> taskName;
	private final Binding<Number> taskDuration;
	private final ResultListCellFactory resultListCellFactory;
	private final Binding<Boolean> taskRunning;
	private final Binding<Boolean> taskScheduled;
	private final Binding<Boolean> taskFinished;
	private final Binding<Boolean> taskNotStarted;
	private final Binding<Boolean> taskSucceeded;
	private final Binding<Boolean> taskFailed;
	private final Binding<Boolean> taskCancelled;
	private final Binding<Number> countOfWarnSeverity;
	private final Binding<Number> countOfCritSeverity;

	public ListView<DiagnosticResult> resultsListView;
	private Subscription resultSubscription;

	@Inject
	public CheckDetailController(ObjectProperty<HealthCheckTask> selectedTask, ResultListCellFactory resultListCellFactory) {
		this.results = EasyBind.wrapList(FXCollections.observableArrayList());
		this.taskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.taskName = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::getTitle).orElse("");
		this.taskDuration = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::durationInMillisProperty).orElse(-1L);
		this.resultListCellFactory = resultListCellFactory;
		this.taskRunning = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::runningProperty).orElse(false); //TODO: DOES NOT WORK
		this.taskScheduled = taskState.map(Worker.State.SCHEDULED::equals).orElse(false);
		this.taskNotStarted = taskState.map(Worker.State.READY::equals).orElse(false);
		this.taskSucceeded = taskState.map(Worker.State.SUCCEEDED::equals).orElse(false);
		this.taskFailed = taskState.map(Worker.State.FAILED::equals).orElse(false);
		this.taskCancelled = taskState.map(Worker.State.CANCELLED::equals).orElse(false);
		this.taskFinished = EasyBind.combine(taskSucceeded, taskFailed, taskCancelled, (a, b, c) -> a || b || c);
		this.countOfWarnSeverity = results.reduce(countSeverity(DiagnosticResult.Severity.WARN));
		this.countOfCritSeverity = results.reduce(countSeverity(DiagnosticResult.Severity.CRITICAL));
		selectedTask.addListener(this::selectedTaskChanged);
	}

	private void selectedTaskChanged(ObservableValue<? extends HealthCheckTask> observable, HealthCheckTask oldValue, HealthCheckTask newValue) {
		if (resultSubscription != null) {
			resultSubscription.unsubscribe();
		}
		if (newValue != null) {
			resultSubscription = EasyBind.bindContent(results, newValue.results());
		}
	}

	private Function<Stream<? extends DiagnosticResult>, Long> countSeverity(DiagnosticResult.Severity severity) {
		return stream -> stream.filter(item -> severity.equals(item.getServerity())).count();
	}

	@FXML
	public void initialize() {
		resultsListView.setItems(results);
		resultsListView.setCellFactory(resultListCellFactory);
	}

	/* Getter/Setter */

	public String getTaskName() {
		return taskName.getValue();
	}

	public Binding<String> taskNameProperty() {
		return taskName;
	}

	public Number getTaskDuration() {
		return taskDuration.getValue();
	}

	public Binding<Number> taskDurationProperty() {
		return taskDuration;
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

	public boolean isTaskRunning() {
		return taskRunning.getValue();
	}

	public Binding<Boolean> taskRunningProperty() {
		return taskRunning;
	}

	public boolean isTaskFinished() {
		return taskFinished.getValue();
	}

	public Binding<Boolean> taskFinishedProperty() {
		return taskFinished;
	}

	public boolean isTaskScheduled() {
		return taskScheduled.getValue();
	}

	public Binding<Boolean> taskScheduledProperty() {
		return taskScheduled;
	}

	public boolean isTaskNotStarted() {
		return taskNotStarted.getValue();
	}

	public Binding<Boolean> taskNotStartedProperty() {
		return taskNotStarted;
	}

	public boolean isTaskSucceeded() {
		return taskSucceeded.getValue();
	}

	public Binding<Boolean> taskSucceededProperty() {
		return taskSucceeded;
	}

	public boolean isTaskFailed() {
		return taskFailed.getValue();
	}

	public Binding<Boolean> taskFailedProperty() {
		return taskFailed;
	}

	public boolean isTaskCancelled() {
		return taskCancelled.getValue();
	}

	public Binding<Boolean> taskCancelledProperty() {
		return taskCancelled;
	}

}
