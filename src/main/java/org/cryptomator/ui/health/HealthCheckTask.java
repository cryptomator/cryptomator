package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;

class HealthCheckTask extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(HealthCheckTask.class);

	private final Path vaultPath;
	private final VaultConfig vaultConfig;
	private final Masterkey masterkey;
	private final SecureRandom csprng;
	private final HealthCheck check;
	private final ObservableList<Result> results;
	private final LongProperty durationInMillis;
	private final BooleanProperty chosenForExecution;

	public HealthCheckTask(Path vaultPath, VaultConfig vaultConfig, Masterkey masterkey, SecureRandom csprng, HealthCheck check, ResourceBundle resourceBundle) {
		this.vaultPath = Objects.requireNonNull(vaultPath);
		this.vaultConfig = Objects.requireNonNull(vaultConfig);
		this.masterkey = Objects.requireNonNull(masterkey);
		this.csprng = Objects.requireNonNull(csprng);
		this.check = Objects.requireNonNull(check);
		this.results = FXCollections.observableArrayList(Result::observables);
		try {
			updateTitle(resourceBundle.getString("health." + check.identifier()));
		} catch (MissingResourceException e) {
			LOG.warn("Missing proper name for health check {}, falling back to default.", check.identifier());
			updateTitle(check.identifier());
		}
		this.durationInMillis = new SimpleLongProperty(-1);
		this.chosenForExecution = new SimpleBooleanProperty();
	}

	@Override
	protected Void call() {
		Instant start = Instant.now();
		try (var masterkeyClone = masterkey.clone(); //
			 var cryptor = CryptorProvider.forScheme(vaultConfig.getCipherCombo()).provide(masterkeyClone, csprng)) {
			check.check(vaultPath, vaultConfig, masterkeyClone, cryptor, diagnosis -> {
				if (isCancelled()) {
					throw new CancellationException();
				}
				Platform.runLater(() -> results.add(Result.create(diagnosis)));
			});
		}
		Platform.runLater(() -> durationInMillis.set(Duration.between(start, Instant.now()).toMillis()));
		return null;
	}

	@Override
	protected void scheduled() {
		LOG.info("starting {}", check.identifier());
	}

	@Override
	protected void done() {
		LOG.info("finished {}", check.identifier());
	}

	/* Getter */

	Observable[] observables() {
		return new Observable[]{results, chosenForExecution};
	}

	public ObservableList<Result> results() {
		return results;
	}

	public HealthCheck getCheck() {
		return check;
	}

	public LongProperty durationInMillisProperty() {
		return durationInMillis;
	}

	public long getDurationInMillis() {
		return durationInMillis.get();
	}

	public BooleanProperty chosenForExecutionProperty() {
		return chosenForExecution;
	}

	public boolean isChosenForExecution() {
		return chosenForExecution.get();
	}
}
