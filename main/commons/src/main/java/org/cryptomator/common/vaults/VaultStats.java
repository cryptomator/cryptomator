package org.cryptomator.common.vaults;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@PerVault
public class VaultStats {

	private static final Logger LOG = LoggerFactory.getLogger(VaultStats.class);

	private final AtomicReference<CryptoFileSystem> fs;
	private final ObjectProperty<VaultState> state;
	private final ScheduledService<Optional<CryptoFileSystemStats>> updateService;
	private final LongProperty bytesPerSecondRead = new SimpleLongProperty();
	private final LongProperty bytesPerSecondWritten = new SimpleLongProperty();
	private final LongProperty bytesPerSecondEncrypted = new SimpleLongProperty();
	private final LongProperty bytesPerSecondDecrypted = new SimpleLongProperty();
	private final DoubleProperty cacheHitRate = new SimpleDoubleProperty();
	//private final IntegerProperty filesRead = new SimpleIntegerProperty();
	//private final IntegerProperty filesWritten = new SimpleIntegerProperty();

	@Inject
	VaultStats(AtomicReference<CryptoFileSystem> fs, ObjectProperty<VaultState> state, ExecutorService executor) {
		this.fs = fs;
		this.state = state;
		this.updateService = new UpdateStatsService();
		updateService.setExecutor(executor);
		updateService.setPeriod(Duration.seconds(1));

		state.addListener(this::vaultStateChanged);
	}

	private void vaultStateChanged(@SuppressWarnings("unused") Observable observable) {
		if (VaultState.UNLOCKED.equals(state.get())) {
			assert fs.get() != null;
			LOG.debug("start recording stats");
			updateService.restart();
		} else {
			LOG.debug("stop recording stats");
			updateService.cancel();
		}
	}

	private void updateStats(Optional<CryptoFileSystemStats> stats) {
		assert Platform.isFxApplicationThread();
		bytesPerSecondRead.set(stats.map(CryptoFileSystemStats::pollBytesRead).orElse(0l));
		bytesPerSecondWritten.set(stats.map(CryptoFileSystemStats::pollBytesWritten).orElse(0l));
		cacheHitRate.set(stats.map(this::getCacheHitRate).orElse(0.0));
		bytesPerSecondDecrypted.set(stats.map(CryptoFileSystemStats::pollBytesDecrypted).orElse(0l));
		bytesPerSecondEncrypted.set(stats.map(CryptoFileSystemStats::pollBytesEncrypted).orElse(0l));
	}
	
	private double getCacheHitRate(CryptoFileSystemStats stats) {
		long accesses = stats.pollChunkCacheAccesses();
		long hits = stats.pollChunkCacheHits();
		if (accesses == 0) {
			return 0.0;
		} else {
			return hits / (double) accesses;
		}
	}

	private class UpdateStatsService extends ScheduledService<Optional<CryptoFileSystemStats>> {

		@Override
		protected Task<Optional<CryptoFileSystemStats>> createTask() {
			return new Task<>() {
				@Override
				protected Optional<CryptoFileSystemStats> call() {
					return Optional.ofNullable(fs.get()).map(CryptoFileSystem::getStats);
				}
			};
		}

		@Override
		protected void succeeded() {
			assert getValue() != null;
			updateStats(getValue());
			super.succeeded();
		}
	}

	/* Observables */

	public LongProperty bytesPerSecondReadProperty() {
		return bytesPerSecondRead;
	}

	public long getBytesPerSecondRead() {
		return bytesPerSecondRead.get();
	}

	public LongProperty bytesPerSecondWrittenProperty() {
		return bytesPerSecondWritten;
	}

	public long getBytesPerSecondWritten() {
		return bytesPerSecondWritten.get();
	}

	public LongProperty bytesPerSecondEncryptedProperty() {
		return bytesPerSecondEncrypted;
	}

	public long getBytesPerSecondEnrypted() {
		return bytesPerSecondEncrypted.get();
	}

	public LongProperty bytesPerSecondDecryptedProperty() {
		return bytesPerSecondDecrypted;
	}

	public long getBytesPerSecondDecrypted() {
		return bytesPerSecondDecrypted.get();
	}

	public DoubleProperty cacheHitRateProperty() {
		return cacheHitRate;
	}

	public double getCacheHitRate() {
		return cacheHitRate.get();
	}


}
