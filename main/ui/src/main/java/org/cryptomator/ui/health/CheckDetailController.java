package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CheckDetailController implements FxController {

	private final Binding<ObservableList<DiagnosticResult>> selectedResults;
	private final OptionalBinding<Worker.State> selectedTaskState;
	private final Binding<String> selectedTaskName;
	private final Binding<String> selectedTaskDescription;

	public TableView<DiagnosticResult> resultsTableView;
	public TableColumn<DiagnosticResult, String> resultDescriptionColumn;
	public TableColumn<DiagnosticResult, String> resultSeverityColumn;

	@Inject
	public CheckDetailController(ObjectProperty<HealthCheckTask> selectedTask) {
		this.selectedResults = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::results).orElse(FXCollections.emptyObservableList());
		this.selectedTaskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.selectedTaskName = EasyBind.wrapNullable(selectedTask).map(HealthCheckTask::getTitle).orElse("");
		this.selectedTaskDescription = EasyBind.wrapNullable(selectedTask).map(task -> task.getCheck().toString()).orElse("");
	}

	@FXML
	public void initialize() {
		resultsTableView.itemsProperty().bind(selectedResults);
		resultDescriptionColumn.setCellValueFactory(cellFeatures -> new SimpleStringProperty(cellFeatures.getValue().toString()));
		resultSeverityColumn.setCellValueFactory(cellFeatures -> new SimpleStringProperty(cellFeatures.getValue().getServerity().name()));
	}

	/* Getter/Setter */

	public String getSelectedTaskName() {
		return selectedTaskName.getValue();
	}

	public Binding<String> selectedTaskNameProperty() {
		return selectedTaskName;
	}

	public String getSelectedTaskDescription() {
		return selectedTaskDescription.getValue();
	}

	public Binding<String> selectedTaskDescriptionProperty() {
		return selectedTaskDescription;
	}

}
