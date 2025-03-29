package org.cryptomator.common.vaults;

import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@PerVault
public class VaultStats {

	private static final Logger LOG = LoggerFactory.getLogger(VaultStats.class);

	private final AtomicReference<CryptoFileSystem> fs;
	private final VaultState state;
	private final ScheduledService<Optional<CryptoFileSystemStats>> updateService;
	private final LongProperty bytesPerSecondRead = new SimpleLongProperty();
	private final LongProperty bytesPerSecondWritten = new SimpleLongProperty();
	private final LongProperty bytesPerSecondEncrypted = new SimpleLongProperty();
	private final LongProperty bytesPerSecondDecrypted = new SimpleLongProperty();
	private final DoubleProperty cacheHitRate = new SimpleDoubleProperty();
	private final LongProperty totalBytesRead = new SimpleLongProperty();
	private final LongProperty totalBytesWritten = new SimpleLongProperty();
	private final LongProperty totalBytesEncrypted = new SimpleLongProperty();
	private final LongProperty totalBytesDecrypted = new SimpleLongProperty();
	private final LongProperty filesRead = new SimpleLongProperty();
	private final LongProperty filesWritten = new SimpleLongProperty();
	private final LongProperty filesAccessed = new SimpleLongProperty();
	private final LongProperty totalFilesAccessed = new SimpleLongProperty();
	private final ObjectProperty<Instant> lastActivity = new SimpleObjectProperty<>();
	private final LongProperty totalSize = new SimpleLongProperty(); // Paa2f

	@Inject
	VaultStats(AtomicReference<CryptoFileSystem> fs, VaultState state, ExecutorService executor) {
		this.fs = fs;
		this.state = state;
		this.updateService = new UpdateStatsService();
		updateService.setExecutor(executor);
		updateService.setPeriod(Duration.seconds(1));

		state.addListener(this::vaultStateChanged);
	}

	private void vaultStateChanged(@SuppressWarnings("unused") Observable observable) {
		if (VaultState.Value.UNLOCKED == state.get()) {
			assert fs.get() != null;
			LOG.debug("start recording stats");
			Platform.runLater(() -> {
				lastActivity.set(Instant.now());
				updateService.restart();
			});
		} else {
			LOG.debug("stop recording stats");
			Platform.runLater(() -> updateService.cancel());
		}
	}

	private void updateStats(Optional<CryptoFileSystemStats> stats) {
		assert Platform.isFxApplicationThread();
		bytesPerSecondRead.set(stats.map(CryptoFileSystemStats::pollBytesRead).orElse(0L));
		bytesPerSecondWritten.set(stats.map(CryptoFileSystemStats::pollBytesWritten).orElse(0L));
		cacheHitRate.set(stats.map(this::getCacheHitRate).orElse(0.0));
		bytesPerSecondDecrypted.set(stats.map(CryptoFileSystemStats::pollBytesDecrypted).orElse(0L));
		bytesPerSecondEncrypted.set(stats.map(CryptoFileSystemStats::pollBytesEncrypted).orElse(0L));
		totalBytesRead.set(stats.map(CryptoFileSystemStats::pollTotalBytesRead).orElse(0L));
		totalBytesWritten.set(stats.map(CryptoFileSystemStats::pollTotalBytesWritten).orElse(0L));
		totalBytesEncrypted.set(stats.map(CryptoFileSystemStats::pollTotalBytesEncrypted).orElse(0L));
		totalBytesDecrypted.set(stats.map(CryptoFileSystemStats::pollTotalBytesDecrypted).orElse(0L));
		var oldAccessCount = filesRead.get() + filesWritten.get();
		filesRead.set(stats.map(CryptoFileSystemStats::pollAmountOfAccessesRead).orElse(0L));
		filesWritten.set(stats.map(CryptoFileSystemStats::pollAmountOfAccessesWritten).orElse(0L));
		filesAccessed.set(stats.map(CryptoFileSystemStats::pollAmountOfAccesses).orElse(0L));
		totalFilesAccessed.set(stats.map(CryptoFileSystemStats::pollTotalAmountOfAccesses).orElse(0L));
		var newAccessCount = filesRead.get() + filesWritten.get();

		// check for any I/O activity
		if (newAccessCount > oldAccessCount) {
			lastActivity.set(Instant.now());
			totalSize.set(stats.map(CryptoFileSystemStats::pollTotalSize).orElse(0L)); // Pf229
		}
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

		private UpdateStatsService() {
			setOnFailed(event -> LOG.error("Error in UpdateStateService.", getException()));
		}

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

	public long getBytesPerSecondEncrypted() {
		return bytesPerSecondEncrypted.get();
	}

	public LongProperty bytesPerSecondDecryptedProperty() {
		return bytesPerSecondDecrypted;
	}

	public long getBytesPerSecondDecrypted() {
		return bytesPerSecondDecrypted.get();
	}

	public DoubleProperty cacheHitRateProperty() { return cacheHitRate; }

	public double getCacheHitRate() {
		return cacheHitRate.get();
	}

	public LongProperty totalBytesReadProperty() {return totalBytesRead;}

	public long getTotalBytesRead() { return totalBytesRead.get();}

	public LongProperty totalBytesWrittenProperty() {return totalBytesWritten;}

	public long getTotalBytesWritten() { return totalBytesWritten.get();}

	public LongProperty totalBytesEncryptedProperty() {return totalBytesEncrypted;}

	public long getTotalBytesEncrypted() { return totalBytesEncrypted.get();}

	public LongProperty totalBytesDecryptedProperty() {return totalBytesDecrypted;}

	public long getTotalBytesDecrypted() { return totalBytesDecrypted.get();}

	public LongProperty filesRead() { return filesRead;}

	public long getFilesRead() { return filesRead.get();}

	public LongProperty filesWritten() {return filesWritten;}

	public long getFilesWritten() {return filesWritten.get();}

	public LongProperty filesAccessed() {
		return filesAccessed;}

	public long getFilesAccessed() {return filesAccessed.get();}

	public LongProperty totalFilesAccessed(){
		return totalFilesAccessed;
	}

	public long getTotalFilesAccessed(){
		return totalFilesAccessed.get();
	}

	public ObjectProperty<Instant> lastActivityProperty() {
		return lastActivity;
	}

	public Instant getLastActivity() {
		return lastActivity.get();
	}

	public LongProperty totalSizeProperty() { // P6122
		return totalSize;
	}

	public long getTotalSize() {
		return totalSize.get();
	}
}
