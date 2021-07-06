package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.HealthCheck;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Check {

	private static final String LOCALIZE_PREFIX = "health.";

	private final HealthCheck check;
	private final ResourceBundle resourceBundle;

	private final BooleanProperty chosenForExecution = new SimpleBooleanProperty(false);
	private final ObjectProperty<CheckState> state = new SimpleObjectProperty<>(CheckState.RUNNABLE);
	private final ObservableList<Result> results = FXCollections.observableArrayList(Result::observables);
	private final ObjectProperty<Throwable> error = new SimpleObjectProperty<>(null);
	private final BooleanBinding isInReRunState = state.isNotEqualTo(CheckState.RUNNING).or(state.isNotEqualTo(CheckState.SCHEDULED));

	Check(HealthCheck check, ResourceBundle resourceBundle) {
		this.check = check;
		this.resourceBundle = resourceBundle;
	}

	String getLocalizedName() {
		try {
			return resourceBundle.getString(LOCALIZE_PREFIX+check.identifier());
		} catch (MissingResourceException e){
			return check.identifier();
		}
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

	ObjectProperty stateProperty() {
		return state;
	}

	CheckState getState() {
		return state.get();
	}

	void setState(CheckState newState) {
		state.set(newState);
	}

	ObjectProperty errorProperty() {
		return error;
	}

	Throwable getError() {
		return error.get();
	}

	void setError(Throwable t) {
		error.set(t);
	}

	boolean isInReRunState() {
		return isInReRunState.get();
	}

	enum CheckState {
		RUNNABLE,
		SCHEDULED,
		RUNNING,
		WITH_CRITICALS, //TODO: maybe the highest result represnt by property and only use one state
		WITH_WARNINGS,
		ALL_GOOD,
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
