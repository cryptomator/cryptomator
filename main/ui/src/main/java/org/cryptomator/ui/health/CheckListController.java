package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import dagger.Lazy;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

@HealthCheckScoped
public class CheckListController implements FxController {

	private final ObservableList<HealthCheckTask> tasks;
	private final HealthReportWriteTask reportWriter;
	private final ExecutorService executorService;
	private final ObjectProperty<HealthCheckTask> selectedTask;
	private final SimpleObjectProperty<Worker<?>> runningTask;
	private final Binding<Boolean> running;
	private final BooleanBinding anyCheckSelected;

	/* FXML */
	public ListView<HealthCheckTask> checksListView;

	@Inject
	public CheckListController(Lazy<Collection<HealthCheckTask>> tasks, HealthReportWriteTask reportWriteTask, ObjectProperty<HealthCheckTask> selectedTask, ExecutorService executorService) {
		this.tasks = FXCollections.observableArrayList(tasks.get());
		this.reportWriter = reportWriteTask;
		this.executorService = executorService;
		this.selectedTask = selectedTask;
		this.runningTask = new SimpleObjectProperty<>();
		this.running = EasyBind.wrapNullable(runningTask).map(Worker::isRunning).orElse(false);
		this.anyCheckSelected = selectedTask.isNotNull();
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
		executorService.execute(reportWriter);
	}

	/* Getter/Setter */

	public boolean isRunning() {
		return running.getValue();
	}

	public Binding<Boolean> runningProperty() {
		return running;
	}

	public boolean isAnyCheckSelected() {
		return anyCheckSelected.get();
	}

	public BooleanBinding anyCheckSelectedProperty() {
		return anyCheckSelected;
	}

}
