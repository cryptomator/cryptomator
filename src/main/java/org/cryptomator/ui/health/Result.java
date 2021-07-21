package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.health.api.DiagnosticResult;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

record Result(DiagnosticResult diagnosis, ObjectProperty<FixState> fixState) {

	enum FixState {
		NOT_FIXABLE,
		FIXABLE,
		FIXING,
		FIXED,
		FIX_FAILED
	}

	public static Result create(DiagnosticResult diagnosis) {
		FixState initialState = diagnosis.getSeverity() == DiagnosticResult.Severity.WARN ? FixState.FIXABLE : FixState.NOT_FIXABLE;
		return new Result(diagnosis, new SimpleObjectProperty<>(initialState));
	}

	public Observable[] observables() {
		return new Observable[]{fixState};
	}

	public String getDescription() {
		return diagnosis.toString();
	}

	public FixState getState() {
		return fixState.get();
	}

	public void setState(FixState state) {
		this.fixState.set(state);
	}

}
