package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import com.tobiasdiez.easybind.EasyBind;
import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.io.IOException;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@HealthCheckScoped
public class CheckController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckController.class);
	private static final Set<Worker.State> endStates = Set.of(Worker.State.FAILED, Worker.State.CANCELLED, Worker.State.SUCCEEDED);

	private final ObservableList<HealthCheckTask> tasks;
	private final ReportWriter reportWriter;
	private final ExecutorService executorService;
	private final ObjectProperty<HealthCheckTask> selectedTask;
	private final ResourceBundle resourceBundle;
	private final SimpleObjectProperty<Worker<?>> runningTask;
	private final Binding<Boolean> running;
	private final Binding<Boolean> finished;
	private final BooleanBinding anyCheckSelected;
	private final BooleanBinding readyToRun;
	private final StringProperty runButtonDescription;

	/* FXML */
	public ListView<HealthCheckTask> checksListView;


	@Inject
	public CheckController(Lazy<Collection<HealthCheckTask>> tasks, ReportWriter reportWriteTask, ObjectProperty<HealthCheckTask> selectedTask, ExecutorService executorService, ResourceBundle resourceBundle) {
		this.tasks = FXCollections.observableArrayList(tasks.get());
		this.reportWriter = reportWriteTask;
		this.executorService = executorService;
		this.selectedTask = selectedTask;
		this.resourceBundle = resourceBundle;
		this.runningTask = new SimpleObjectProperty<>();
		this.running = EasyBind.wrapNullable(runningTask).mapObservable(Worker::runningProperty).orElse(false);
		this.finished = EasyBind.wrapNullable(runningTask).mapObservable(Worker::stateProperty).map(endStates::contains).orElse(false);
		this.readyToRun = runningTask.isNull();
		this.anyCheckSelected = selectedTask.isNotNull();
		this.runButtonDescription = new SimpleStringProperty(resourceBundle.getString("health.check.runAllButton"));
	}

	@FXML
	public void initialize() {
		checksListView.setItems(tasks);
		checksListView.setCellFactory(ignored -> new CheckListCell());
		selectedTask.bind(checksListView.getSelectionModel().selectedItemProperty());
		selectedTask.addListener(this::updateRunButtonDescription);
	}

	private void updateRunButtonDescription(ObservableValue<? extends HealthCheckTask> observable, HealthCheckTask oldTask, HealthCheckTask newTask) {
		if (newTask == null) {
			runButtonDescription.set(resourceBundle.getString("health.check.runSingleButton"));
		} else if (oldTask == null && newTask != null) {
			runButtonDescription.set(resourceBundle.getString("health.check.runAllButton"));
		}
	}

	@FXML
	public synchronized void runSelectedChecks() {
		startBatch(checksListView.getSelectionModel().getSelectedItems());
	}

	@FXML
	public synchronized void runAllChecks() {
		startBatch(checksListView.getItems());
	}

	private void startBatch(Iterable<HealthCheckTask> batch) {
		Preconditions.checkState(runningTask.get() == null);
		var batchService = new BatchService(batch);
		batchService.setExecutor(executorService);
		batchService.start();
		runningTask.set(batchService);
	}

	@FXML
	public synchronized void cancelCheck() {
		Preconditions.checkState(runningTask.get() != null);
		runningTask.get().cancel();
	}

	@FXML
	public void exportResults() {
		try {
			reportWriter.writeReport(tasks);
		} catch (IOException e) {
			//TODO: better error handling
			LOG.error("Failed to write health check report.", e);
		}
	}
	/* Getter/Setter */


	public boolean isReadyToRun() {
		return readyToRun.get();
	}

	public BooleanBinding readyToRunProperty() {
		return readyToRun;
	}

	public boolean isRunning() {
		return running.getValue();
	}

	public Binding<Boolean> runningProperty() {
		return running;
	}

	public boolean isFinished() {
		return finished.getValue();
	}

	public Binding<Boolean> finishedProperty() {
		return finished;
	}

	public boolean isAnyCheckSelected() {
		return anyCheckSelected.get();
	}

	public BooleanBinding anyCheckSelectedProperty() {
		return anyCheckSelected;
	}

}
