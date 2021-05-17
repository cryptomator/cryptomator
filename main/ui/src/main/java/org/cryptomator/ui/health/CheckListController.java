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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@HealthCheckScoped
public class CheckListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckListController.class);
	private static final Set<Worker.State> endStates = Set.of(Worker.State.FAILED, Worker.State.CANCELLED, Worker.State.SUCCEEDED);

	private final ObservableList<HealthCheckTask> tasks;
	private final ReportWriter reportWriter;
	private final ExecutorService executorService;
	private final ObjectProperty<HealthCheckTask> selectedTask;
	private final SimpleObjectProperty<Worker<?>> runningTask;
	private final Binding<Boolean> running;
	private final Binding<Boolean> finished;
	private final BooleanBinding anyCheckSelected;
	private final BooleanBinding readyToRun;

	/* FXML */
	public ListView<HealthCheckTask> checksListView;


	@Inject
	public CheckListController(Lazy<Collection<HealthCheckTask>> tasks, ReportWriter reportWriteTask, ObjectProperty<HealthCheckTask> selectedTask, ExecutorService executorService) {
		this.tasks = FXCollections.observableArrayList(tasks.get());
		this.reportWriter = reportWriteTask;
		this.executorService = executorService;
		this.selectedTask = selectedTask;
		this.runningTask = new SimpleObjectProperty<>();
		this.running = EasyBind.wrapNullable(runningTask).mapObservable(Worker::runningProperty).orElse(false);
		this.finished = EasyBind.wrapNullable(runningTask).mapObservable(Worker::stateProperty).map(endStates::contains).orElse(false);
		this.anyCheckSelected = selectedTask.isNotNull();
		this.readyToRun = runningTask.isNull();
	}

	@FXML
	public void initialize() {
		checksListView.setItems(tasks);
		checksListView.setCellFactory(ignored -> new CheckListCell());
		selectedTask.bind(checksListView.getSelectionModel().selectedItemProperty());
	}

	@FXML
	public synchronized void runSelectedChecks() {
		Preconditions.checkState(runningTask.get() == null);
		var batch = new BatchService(checksListView.getSelectionModel().getSelectedItems());
		batch.setExecutor(executorService);
		batch.start();
		runningTask.set(batch);
	}

	@FXML
	public synchronized void runAllChecks() {
		Preconditions.checkState(runningTask.get() == null);
		var batch = new BatchService(checksListView.getItems());
		batch.setExecutor(executorService);
		batch.start();
		runningTask.set(batch);
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
