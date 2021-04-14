package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import dagger.Lazy;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class CheckController implements FxController {

	private final Stage window;
	private final VaultConfig vaultConfig;
	private final ExecutorService executor;
	private final ObjectProperty<HealthCheckTask> selectedTask;
	private final ObservableList<HealthCheckTask> tasks;
	private final ObjectBinding<ObservableList<DiagnosticResult>> selectedResults;
	private final OptionalBinding<Worker.State> selectedTaskState;
	private final BooleanExpression ready;
	private final BooleanExpression running;

	public ListView<HealthCheckTask> checksListView;
	public ListView<DiagnosticResult> resultsListView;


	@Inject
	public CheckController(@HealthCheckWindow Stage window, AtomicReference<VaultConfig> vaultConfigRef, Lazy<Collection<HealthCheckTask>> tasks, ExecutorService executor, ObjectProperty<HealthCheckTask> selectedTask) {
		this.window = window;
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.executor = executor;
		this.selectedTask = selectedTask;
		this.selectedResults = Bindings.createObjectBinding(this::getSelectedResults, selectedTask);
		this.selectedTaskState = EasyBind.wrapNullable(selectedTask).mapObservable(HealthCheckTask::stateProperty);
		this.ready = BooleanExpression.booleanExpression(selectedTaskState.map(Worker.State.READY::equals).orElse(false));
		this.running = BooleanExpression.booleanExpression(selectedTaskState.map(Worker.State.RUNNING::equals).orElse(false));
		this.tasks = FXCollections.observableArrayList(tasks.get());
	}

	@FXML
	public void initialize() {
		checksListView.setItems(tasks);
		checksListView.setCellFactory(ignored -> new CheckListCell());
		resultsListView.itemsProperty().bind(selectedResults);
		resultsListView.setCellFactory(ignored -> new ResultListCell());
		selectedTask.bind(checksListView.getSelectionModel().selectedItemProperty());
	}

	@FXML
	public void runCheck() {
		assert selectedTask.get() != null;
		executor.execute(selectedTask.get());
	}
	@FXML
	public void cancelCheck() {
		assert selectedTask.get() != null;
		assert selectedTask.get().isRunning();
		selectedTask.get().cancel();
	}
	/* Getter/Setter */

	public VaultConfig getVaultConfig() {
		return vaultConfig;
	}

	public boolean isRunning() {
		return running.get();
	}

	public BooleanExpression runningProperty() {
		return running;
	}

	public boolean isReady() {
		return ready.get();
	}

	public BooleanExpression readyProperty() {
		return ready;
	}

	private ObservableList<DiagnosticResult> getSelectedResults() {
		if (selectedTask.get() == null) {
			return FXCollections.emptyObservableList();
		} else {
			return selectedTask.get().results();
		}
	}
}
