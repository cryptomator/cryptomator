package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.Masterkey;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.nio.file.Path;

record Result(DiagnosticResult diagnosis, ObjectProperty<FixState> fixState) {

	enum FixState {
		NOT_FIXABLE,
		FIXABLE,
		FIXING,
		FIXED,
		FIX_FAILED
	}

	public static Result create(DiagnosticResult diagnosis, Path vaultPath, VaultConfig config, Masterkey masterkey, Cryptor cryptor) {
		FixState initialState = diagnosis.getFix(vaultPath, config, masterkey, cryptor).map( _f -> FixState.FIXABLE).orElse(FixState.NOT_FIXABLE);
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
