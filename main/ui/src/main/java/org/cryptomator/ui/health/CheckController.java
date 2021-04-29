package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.concurrent.ExecutorService;

@HealthCheckScoped
public class CheckController implements FxController {

	private final HealthCheckSupervisor supervisor;
	private final HealthReportWriteTask reportWriter;
	private final ExecutorService executorService;
	private final ObjectProperty<HealthCheckTask> selectedTask;
	private final Binding<ObservableList<DiagnosticResult>> selectedResults;
	private final OptionalBinding<Worker.State> selectedTaskState;
	private final Binding<String> selectedTaskName;
	private final Binding<String> selectedTaskDescription;
	private final ReadOnlyBooleanProperty running;

	/* FXML */
	public ListView<HealthCheckTask> checksListView;
	public TableView<DiagnosticResult> resultsTableView;
	public TableColumn<DiagnosticResult, String> resultDescriptionColumn;
	public TableColumn<DiagnosticResult, String> resultSeverityColumn;


	@Inject
	public CheckController(HealthCheckSupervisor supervisor, HealthReportWriteTask reportWriteTask, ExecutorService executorService) {
		this.supervisor = supervisor;
		this.reportWriter = reportWriteTask;
		this.executorService = executorService;
		this.selectedTask = new SimpleObjectProperty<>();
		this.selectedResults = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::results).orElse(FXCollections.emptyObservableList());
		this.selectedTaskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.selectedTaskName = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::getTitle).orElse("");
		this.selectedTaskDescription = EasyBind.wrapNullable(selectedTask).map(task -> task.getCheck().toString()).orElse("");
		this.running = supervisor.runningProperty();
	}

	@FXML
	public void initialize() {
		checksListView.setItems(FXCollections.observableArrayList(supervisor.getTasks()));
		checksListView.setCellFactory(ignored -> new CheckListCell());
		checksListView.getSelectionModel().select(0);
		selectedTask.bind(checksListView.getSelectionModel().selectedItemProperty());

		resultsTableView.itemsProperty().bind(selectedResults);
		resultDescriptionColumn.setCellValueFactory(cellFeatures -> new SimpleStringProperty(cellFeatures.getValue().toString()));
		resultSeverityColumn.setCellValueFactory(cellFeatures -> new SimpleStringProperty(cellFeatures.getValue().getServerity().name()));
		executorService.execute(supervisor);
	}

	@FXML
	public void cancelCheck() {
		assert running.get();
		supervisor.cancel(true);
	}


	@FXML
	public void exportResults() {
		executorService.execute(reportWriter);
	}

	/* Getter&Setter */

	public boolean isRunning() {
		return running.get();
	}

	public ReadOnlyBooleanProperty runningProperty() {
		return running;
	}

	public Binding<String> selectedTaskNameProperty() {
		return selectedTaskName;
	}

	public String isSelectedTaskName() {
		return selectedTaskName.getValue();
	}

	public Binding<String> selectedTaskDescriptionProperty() {
		return selectedTaskDescription;
	}

	public String isSelectedTaskDescription() {
		return selectedTaskDescription.getValue();
	}
}
