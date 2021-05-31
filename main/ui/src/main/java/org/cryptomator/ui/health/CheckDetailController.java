package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
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

	private final Map<ObservableList<DiagnosticResult>, WarnAndErrorEntry> cachedWarnAndErrorCounts;
	private final Binding<ObservableList<DiagnosticResult>> results;
	private final OptionalBinding<Worker.State> taskState;
	private final Binding<String> taskName;
	private final Binding<Number> taskDuration;
	private final ResultListCellFactory resultListCellFactory;
	private final BooleanBinding producingResults;
	private final LongProperty numOfWarnings;
	private final LongProperty numOfErrors;

	public ListView<DiagnosticResult> resultsListView;

	@Inject
	public CheckDetailController(ObjectProperty<HealthCheckTask> selectedTask, ResultListCellFactory resultListCellFactory) {
		selectedTask.addListener(this::rebindWarnAndErrorCount);
		this.results = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::results).orElse(FXCollections.emptyObservableList());
		this.taskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.taskName = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::getTitle).orElse("");
		this.taskDuration = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::durationInMillisProperty).orElse(-1L);
		this.resultListCellFactory = resultListCellFactory;
		this.producingResults = taskState.filter(this::producesResults).isPresent();
		this.numOfWarnings = new SimpleLongProperty(0);
		this.numOfErrors = new SimpleLongProperty(0);
		this.cachedWarnAndErrorCounts = new IdentityHashMap<>(); //important to use an identity hashmap, because collections violate the immnutable hashkey contract
	}

	private synchronized void rebindWarnAndErrorCount(ObservableValue<? extends HealthCheckTask> observable, HealthCheckTask oldVal, HealthCheckTask newVal) {
		//create and cache properites for the newList, if not already present
		final var listToUpdate = newVal.results();
		cachedWarnAndErrorCounts.computeIfAbsent(listToUpdate, key -> {
			var warnProperty = new SimpleLongProperty(countSeverityInList(listToUpdate, DiagnosticResult.Severity.WARN));
			var errProperty = new SimpleLongProperty(countSeverityInList(listToUpdate, DiagnosticResult.Severity.CRITICAL));
			return new WarnAndErrorEntry(warnProperty, errProperty);
		});
		listToUpdate.addListener(this::updateListSpecificWarnAndErrorCount);

		//updateBindings
		numOfErrors.bind(cachedWarnAndErrorCounts.get(listToUpdate).errorCount);
		numOfWarnings.bind(cachedWarnAndErrorCounts.get(listToUpdate).warningCount);
	}

	private synchronized void updateListSpecificWarnAndErrorCount(ListChangeListener.Change<? extends DiagnosticResult> c) {
		long tmpErr = cachedWarnAndErrorCounts.get(c.getList()).errorCount.get();
		long tmpWarn = cachedWarnAndErrorCounts.get(c.getList()).warningCount.get();
		while (c.next()) {
			if (c.wasAdded()) {
				tmpWarn += countSeverityInList(c.getAddedSubList(), DiagnosticResult.Severity.WARN);
				tmpErr += countSeverityInList(c.getAddedSubList(), DiagnosticResult.Severity.CRITICAL);
			}
		}
		cachedWarnAndErrorCounts.get(c.getList()).errorCount.set(tmpErr);
		cachedWarnAndErrorCounts.get(c.getList()).warningCount.set(tmpWarn);
	}

	private long countSeverityInList(List<? extends DiagnosticResult> list, DiagnosticResult.Severity severityToCount) {
		return list.stream().map(DiagnosticResult::getServerity).filter(severityToCount::equals).count();
	}

	private boolean producesResults(Worker.State state) {
		return switch (state) {
			case SCHEDULED, RUNNING -> true;
			case READY, SUCCEEDED, CANCELLED, FAILED -> false;
		};
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

	public boolean isProducingResults() {
		return producingResults.get();
	}

	public BooleanBinding producingResultsProperty() {
		return producingResults;
	}

	public Number getTaskDuration() {
		return taskDuration.getValue();
	}

	public Binding<Number> taskDurationProperty() {
		return taskDuration;
	}

	public long getNumOfWarnings() {
		return numOfWarnings.get();
	}

	public LongProperty numOfWarningsProperty() {
		return numOfWarnings;
	}

	public long getNumOfErrors() {
		return numOfErrors.get();
	}

	public LongProperty numOfErrorsProperty() {
		return numOfErrors;
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
