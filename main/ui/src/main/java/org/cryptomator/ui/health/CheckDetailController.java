package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
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

@HealthCheckScoped
public class CheckDetailController implements FxController {

	private final Binding<ObservableList<DiagnosticResult>> results;
	private final OptionalBinding<Worker.State> taskState;
	private final Binding<String> taskName;
	private final ResultListCellFactory resultListCellFactory;
	private final BooleanBinding producingResults;

	public ListView<DiagnosticResult> resultsListView;

	@Inject
	public CheckDetailController(ObjectProperty<HealthCheckTask> selectedTask, ResultListCellFactory resultListCellFactory) {
		this.results = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::results).orElse(FXCollections.emptyObservableList());
		this.taskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.taskName = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::getTitle).orElse("");
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

	public boolean isProducingResults() {
		return producingResults.get();
	}

	public BooleanBinding producingResultsProperty() {
		return producingResults;
	}
}
