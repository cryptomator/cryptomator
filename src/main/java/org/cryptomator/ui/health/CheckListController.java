package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

@HealthCheckScoped
public class CheckListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CheckListController.class);

	private final Stage window;
	private final ObservableList<Check> checks;
	private final CheckExecutor checkExecutor;
	private final FilteredList<Check> chosenChecks;
	private final ReportWriter reportWriter;
	private final ObjectProperty<Check> selectedCheck;
	private final BooleanBinding mainRunStarted; //TODO: rerunning not considered for now
	private final BooleanBinding somethingsRunning;
	private final FxApplicationWindows appWindows;
	private final IntegerBinding chosenTaskCount;
	private final BooleanBinding anyCheckSelected;
	private final CheckListCellFactory listCellFactory;

	/* FXML */
	public ListView<Check> checksListView;

	@Inject
	public CheckListController(@HealthCheckWindow Stage window, List<Check> checks, CheckExecutor checkExecutor, ReportWriter reportWriteTask, ObjectProperty<Check> selectedCheck, FxApplicationWindows appWindows, CheckListCellFactory listCellFactory) {
		this.window = window;
		this.checks = FXCollections.observableList(checks, Check::observables);
		this.checkExecutor = checkExecutor;
		this.listCellFactory = listCellFactory;
		this.chosenChecks = this.checks.filtered(Check::isChosenForExecution);
		this.reportWriter = reportWriteTask;
		this.selectedCheck = selectedCheck;
		this.appWindows = appWindows;
		this.chosenTaskCount = Bindings.size(this.chosenChecks);
		this.mainRunStarted = Bindings.isEmpty(this.checks.filtered(c -> c.getState() == Check.CheckState.RUNNABLE));
		this.somethingsRunning = Bindings.isNotEmpty(this.checks.filtered(c -> c.getState() == Check.CheckState.SCHEDULED || c.getState() == Check.CheckState.RUNNING));
		this.anyCheckSelected = selectedCheck.isNotNull();
	}

	@FXML
	public void initialize() {
		checksListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		checksListView.setItems(checks);
		checksListView.setCellFactory(listCellFactory);
		selectedCheck.bind(checksListView.getSelectionModel().selectedItemProperty());
	}

	@FXML
	public void selectAllChecks() {
		checks.forEach(t -> t.chosenForExecutionProperty().set(true));
	}

	@FXML
	public void deselectAllChecks() {
		checks.forEach(t -> t.chosenForExecutionProperty().set(false));
	}

	@FXML
	public void runSelectedChecks() {
		Preconditions.checkState(!mainRunStarted.get());
		Preconditions.checkState(!somethingsRunning.get());
		Preconditions.checkState(!chosenChecks.isEmpty());

		checks.filtered(c -> !c.isChosenForExecution()).forEach(c -> c.setState(Check.CheckState.SKIPPED));
		checkExecutor.executeBatch(chosenChecks);
		checksListView.getSelectionModel().select(chosenChecks.get(0));
		checksListView.refresh();
		window.sizeToScene();
	}

	@FXML
	public synchronized void cancelRun() {
		Preconditions.checkState(somethingsRunning.get());
		checkExecutor.cancel();
	}

	@FXML
	public void exportResults() {
		try {
			reportWriter.writeReport(chosenChecks);
		} catch (IOException e) {
			LOG.error("Failed to write health check report.", e);
			appWindows.showErrorWindow(e, window, window.getScene());
		}
	}

	/* Getter/Setter */
	public boolean isRunning() {
		return somethingsRunning.getValue();
	}

	public BooleanBinding runningProperty() {
		return somethingsRunning;
	}

	public boolean isAnyCheckSelected() {
		return anyCheckSelected.get();
	}

	public BooleanBinding anyCheckSelectedProperty() {
		return anyCheckSelected;
	}

	public boolean isMainRunStarted() {
		return mainRunStarted.get();
	}

	public BooleanBinding mainRunStartedProperty() {
		return mainRunStarted;
	}

	public int getChosenTaskCount() {
		return chosenTaskCount.getValue();
	}

	public IntegerBinding chosenTaskCountProperty() {
		return chosenTaskCount;
	}

}
