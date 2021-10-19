package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Check {

	private final HealthCheck check;

	private final BooleanProperty chosenForExecution = new SimpleBooleanProperty(false);
	private final ObjectProperty<CheckState> state = new SimpleObjectProperty<>(CheckState.RUNNABLE);
	private final ObservableList<Result> results = FXCollections.observableArrayList(Result::observables);
	private final ObjectProperty<DiagnosticResult.Severity> highestResultSeverity = new SimpleObjectProperty<>(null);
	private final ObjectProperty<Throwable> error = new SimpleObjectProperty<>(null);
	private final BooleanBinding isInReRunState = state.isNotEqualTo(CheckState.RUNNING).or(state.isNotEqualTo(CheckState.SCHEDULED));

	Check(HealthCheck check) {
		this.check = check;
	}

	String getName() {
		return check.name();
	}

	HealthCheck getHealthCheck() {
		return check;
	}

	BooleanProperty chosenForExecutionProperty() {
		return chosenForExecution;
	}

	boolean isChosenForExecution() {
		return chosenForExecution.get();
	}

	ObjectProperty<CheckState> stateProperty() {
		return state;
	}

	CheckState getState() {
		return state.get();
	}

	void setState(CheckState newState) {
		state.set(newState);
	}

	ObjectProperty<Throwable> errorProperty() {
		return error;
	}

	Throwable getError() {
		return error.get();
	}

	void setError(Throwable t) {
		error.set(t);
	}

	ObjectProperty<DiagnosticResult.Severity> highestResultSeverityProperty() {
		return highestResultSeverity;
	}

	DiagnosticResult.Severity getHighestResultSeverity() {
		return highestResultSeverity.get();
	}

	void setHighestResultSeverity(DiagnosticResult.Severity severity) {
		highestResultSeverity.set(severity);
	}

	boolean isInReRunState() {
		return isInReRunState.get();
	}

	enum CheckState {
		RUNNABLE,
		SCHEDULED,
		RUNNING,
		SUCCEEDED,
		SKIPPED,
		ERROR,
		CANCELLED;
	}

	ObservableList<Result> getResults() {
		return results;
	}

	Observable[] observables() {
		return new Observable[]{chosenForExecution, state, results, error};
	}
}
