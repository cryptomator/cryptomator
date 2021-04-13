package org.cryptomator.ui.health;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class CheckController implements FxController {

	private final Stage window;
	private final VaultConfig vaultConfig;
	private final HealthCheckTaskFactory healthCheckTaskFactory;
	private final ExecutorService executor;
	private final ObjectProperty<HealthCheck> selectedCheck;
	private final ObservableList<HealthCheck> checks;
	private final ObservableList<DiagnosticResult> results;

	public ListView<HealthCheck> checksListView;
	public ListView<DiagnosticResult> resultsListView;

	@Inject
	public CheckController(@HealthCheckWindow Stage window, AtomicReference<VaultConfig> vaultConfigRef, HealthCheckTaskFactory healthCheckTaskFactory, ExecutorService executor, ObjectProperty<HealthCheck> selectedCheck) {
		this.window = window;
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.healthCheckTaskFactory = healthCheckTaskFactory;
		this.executor = executor;
		this.selectedCheck = selectedCheck;
		this.checks = FXCollections.observableArrayList(HealthCheck.allChecks());
		this.results = FXCollections.observableArrayList();
	}

	@FXML
	public void initialize() {
		checksListView.setItems(checks);
		checksListView.setCellFactory(this::createCheckListCell);
		resultsListView.setItems(results);
		resultsListView.setCellFactory(this::createResultListCell);
		selectedCheck.bind(checksListView.getSelectionModel().selectedItemProperty());
	}

	private CheckListCell createCheckListCell(ListView<HealthCheck> list) {
		return new CheckListCell();
	}

	private ResultListCell createResultListCell(ListView<DiagnosticResult> list) {
		return new ResultListCell();
	}

	@FXML
	public void runCheck() {
		executor.execute(healthCheckTaskFactory.newTask(selectedCheck.get(), results::add));
	}

	/* Getter/Setter */

	public VaultConfig getVaultConfig() {
		return vaultConfig;
	}

	public HealthCheck getSelectedCheck() {
		return selectedCheck.get();
	}

	public ReadOnlyObjectProperty<HealthCheck> selectedCheckProperty() {
		return selectedCheck;
	}
}
