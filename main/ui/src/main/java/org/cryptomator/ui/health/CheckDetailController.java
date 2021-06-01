package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@HealthCheckScoped
public class CheckDetailController implements FxController {

	private final Map<ObservableList<DiagnosticResult>, WarnAndErrorEntry> cachedWarnAndCritCounts;
	private final Binding<ObservableList<DiagnosticResult>> results;
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
	private final LongProperty countOfWarnSeverity;
	private final LongProperty countOfCritSeverity;

	public ListView<DiagnosticResult> resultsListView;

	@Inject
	public CheckDetailController(ObjectProperty<HealthCheckTask> selectedTask, ResultListCellFactory resultListCellFactory) {
		selectedTask.addListener(this::rebindWarnAndCritProperties);
		this.results = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::results).orElse(FXCollections.emptyObservableList());
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
		this.countOfWarnSeverity = new SimpleLongProperty(0);
		this.countOfCritSeverity = new SimpleLongProperty(0);
		this.cachedWarnAndCritCounts = new IdentityHashMap<>(); //important to use an identity hashmap, because collections violate the immnutable hashkey contract
	}

	private synchronized void rebindWarnAndCritProperties(ObservableValue<? extends HealthCheckTask> observable, HealthCheckTask oldVal, HealthCheckTask newVal) {
		//create and cache properites for the newList, if not already present
		final var listToUpdate = newVal.results();
		cachedWarnAndCritCounts.computeIfAbsent(listToUpdate, key -> {
			var warnProperty = new SimpleLongProperty(countSeverityInList(listToUpdate, DiagnosticResult.Severity.WARN));
			var errProperty = new SimpleLongProperty(countSeverityInList(listToUpdate, DiagnosticResult.Severity.CRITICAL));
			return new WarnAndErrorEntry(warnProperty, errProperty);
		});
		listToUpdate.addListener(this::updateListSpecificWarnAndCritCount);

		//updateBindings
		countOfCritSeverity.bind(cachedWarnAndCritCounts.get(listToUpdate).errorCount);
		countOfWarnSeverity.bind(cachedWarnAndCritCounts.get(listToUpdate).warningCount);
	}

	private synchronized void updateListSpecificWarnAndCritCount(ListChangeListener.Change<? extends DiagnosticResult> c) {
		long tmpErr = cachedWarnAndCritCounts.get(c.getList()).errorCount.get();
		long tmpWarn = cachedWarnAndCritCounts.get(c.getList()).warningCount.get();
		while (c.next()) {
			if (c.wasAdded()) {
				tmpWarn += countSeverityInList(c.getAddedSubList(), DiagnosticResult.Severity.WARN);
				tmpErr += countSeverityInList(c.getAddedSubList(), DiagnosticResult.Severity.CRITICAL);
			}
		}
		cachedWarnAndCritCounts.get(c.getList()).errorCount.set(tmpErr);
		cachedWarnAndCritCounts.get(c.getList()).warningCount.set(tmpWarn);
	}

	private long countSeverityInList(List<? extends DiagnosticResult> list, DiagnosticResult.Severity severityToCount) {
		return list.stream().map(DiagnosticResult::getServerity).filter(severityToCount::equals).count();
	}

	@FXML
	public void initialize() {
		resultsListView.itemsProperty().bind(results);
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
		return countOfWarnSeverity.get();
	}

	public LongProperty countOfWarnSeverityProperty() {
		return countOfWarnSeverity;
	}

	public long getCountOfCritSeverity() {
		return countOfCritSeverity.get();
	}

	public LongProperty countOfCritSeverityProperty() {
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

	private static class WarnAndErrorEntry {

		WarnAndErrorEntry(LongProperty warningCount, LongProperty errorCount) {
			this.warningCount = warningCount;
			this.errorCount = errorCount;
		}

		final LongProperty warningCount;
		final LongProperty errorCount;
	}

}
