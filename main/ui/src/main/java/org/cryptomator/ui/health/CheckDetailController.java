package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class CheckDetailController implements FxController {

	private final Binding<ObservableList<DiagnosticResultAction>> results;
	private final OptionalBinding<Worker.State> taskState;
	private final Binding<String> taskName;
	private final Binding<String> taskDescription;
	private final ResultListCellFactory resultListCellFactory;
	private final BooleanBinding producingResults;

	public ListView<DiagnosticResultAction> resultsListView;

	@Inject
	public CheckDetailController(ObjectProperty<HealthCheckTask> selectedTask, ResultListCellFactory resultListCellFactory) {
		this.results = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::results).orElse(FXCollections.emptyObservableList());
		this.taskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.taskName = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::getTitle).orElse("");
		this.taskDescription = EasyBind.wrapNullable(selectedTask).map(task -> task.getCheck().toString()).orElse("");
		this.resultListCellFactory = resultListCellFactory;
		this.producingResults = taskState.filter(this::producesResults).isPresent();
	}

	private boolean producesResults(Worker.State state) {
		return switch (state) {
			case SCHEDULED, RUNNING, SUCCEEDED -> true;
			case READY, CANCELLED, FAILED -> false;
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

	public String getTaskDescription() {
		return taskDescription.getValue();
	}

	public Binding<String> taskDescriptionProperty() {
		return taskDescription;
	}

	public boolean isProducingResults() {
		return producingResults.get();
	}

	public BooleanBinding producingResultsProperty() {
		return producingResults;
	}
}
