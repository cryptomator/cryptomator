package org.cryptomator.ui.stats;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultStats;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.WeakBindings;

import javax.inject.Inject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.LongProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private static final int IO_SAMPLING_STEPS = 30;
	private static final double IO_SAMPLING_INTERVAL = 1;

	private final VaultStatisticsComponent component; // keep a strong reference to the component (see component's javadoc)
	private final VaultStats stats;
	private final Series<Number, Number> readData;
	private final Series<Number, Number> writeData;
	private final Series<Number, Number> accessData;
	private final Timeline ioAnimation;
	private final LongBinding bpsRead;
	private final LongBinding bpsWritten;
	private final DoubleBinding cacheHitRate;
	private final DoubleBinding cacheHitDegrees;
	private final DoubleBinding cacheHitPercentage;
	private final LongBinding totalBytesRead;
	private final LongBinding totalBytesWritten;
	private final LongBinding totalBytesEncrypted;
	private final LongBinding totalBytesDecrypted;
	private final LongBinding filesRead;
	private final LongBinding filesWritten;
	private final LongBinding filesAccessed;
	private final LongBinding totalFilesAccessed;
	private final LongBinding bpsEncrypted;
	private final LongBinding bpsDecrypted;
	private final LongBinding totalSize; // P2359

	public AreaChart<Number, Number> readChart;
	public AreaChart<Number, Number> writeChart;
	public AreaChart<Number, Number> accessChart;
	public NumberAxis readChartXAxis;
	public NumberAxis readChartYAxis;
	public NumberAxis writeChartXAxis;
	public NumberAxis writeChartYAxis;
	public NumberAxis accessChartXAxis;
	public NumberAxis accessChartYAxis;

	@Inject
	public VaultStatisticsController(VaultStatisticsComponent component, @VaultStatisticsWindow Stage window, @VaultStatisticsWindow Vault vault) {
		this.component = component;
		this.stats = vault.getStats();
		this.readData = new Series<>();
		this.writeData = new Series<>();
		this.accessData = new Series<>();
		this.bpsRead = WeakBindings.bindLong(stats.bytesPerSecondReadProperty());
		this.bpsWritten = WeakBindings.bindLong(stats.bytesPerSecondWrittenProperty());
		this.cacheHitRate = WeakBindings.bindDouble(stats.cacheHitRateProperty());
		this.cacheHitDegrees = cacheHitRate.multiply(-270);
		this.cacheHitPercentage = cacheHitRate.multiply(100);
		this.totalBytesRead = WeakBindings.bindLong(stats.totalBytesReadProperty());
		this.totalBytesWritten = WeakBindings.bindLong(stats.totalBytesWrittenProperty());
		this.totalBytesDecrypted = WeakBindings.bindLong(stats.totalBytesDecryptedProperty());
		this.totalBytesEncrypted = WeakBindings.bindLong(stats.totalBytesEncryptedProperty());
		this.filesRead = WeakBindings.bindLong(stats.filesRead());
		this.filesWritten = WeakBindings.bindLong(stats.filesWritten());
		this.filesAccessed = WeakBindings.bindLong(stats.filesAccessed());
		this.totalFilesAccessed = WeakBindings.bindLong(stats.totalFilesAccessed());
		this.bpsEncrypted = WeakBindings.bindLong(stats.bytesPerSecondEncryptedProperty());
		this.bpsDecrypted = WeakBindings.bindLong(stats.bytesPerSecondDecryptedProperty());
		this.totalSize = WeakBindings.bindLong(stats.totalSizeProperty()); // Pda50

		this.ioAnimation = new Timeline(); //TODO Research better timer
		ioAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(IO_SAMPLING_INTERVAL), new IoSamplingAnimationHandler(readData, writeData, accessData)));
		ioAnimation.setCycleCount(Animation.INDEFINITE);
		ioAnimation.play();

		// make sure to stop animating while window is closed
		// otherwise a global timer (GC root) will keep a strong reference to animation
		window.setOnHiding(evt -> ioAnimation.stop());
		window.setOnShowing(evt -> ioAnimation.play());
	}

	@FXML
	public void initialize() {
		readChart.getData().addAll(readData);
		writeChart.getData().addAll(writeData);
		accessChart.getData().addAll(accessData);
	}

	private class IoSamplingAnimationHandler implements EventHandler<ActionEvent> {

		private long step = IO_SAMPLING_STEPS;
		private final Series<Number, Number> decryptedBytesRead;
		private final Series<Number, Number> encryptedBytesWrite;
		private final Series<Number, Number> accessedFiles;
		private final long[] maxBuf = new long[IO_SAMPLING_STEPS];
		private final long[] maxAccessBuf = new long[IO_SAMPLING_STEPS];


		public IoSamplingAnimationHandler(Series<Number, Number> readData, Series<Number, Number> writeData, Series<Number, Number> accessData) {
			this.decryptedBytesRead = readData;
			this.encryptedBytesWrite = writeData;
			this.accessedFiles = accessData;

			// initialize data once and change value of data points later:
			for (int i = 0; i < IO_SAMPLING_STEPS; i++) {
				decryptedBytesRead.getData().add(new Data<>(i, 0));
				encryptedBytesWrite.getData().add(new Data<>(i, 0));
				accessedFiles.getData().add(new Data<>(i, 0));
			}
		}

		@Override
		public void handle(ActionEvent event) {
			final long currentStep = step++;
			final long decBytes = stats.bytesPerSecondReadProperty().get();
			final long encBytes = stats.bytesPerSecondWrittenProperty().get();
			final long accFiles = stats.filesAccessed().get();

			maxBuf[(int) currentStep % IO_SAMPLING_STEPS] = Math.max(decBytes, encBytes);
			long allTimeMax = Arrays.stream(maxBuf).max().orElse(0L);
			maxAccessBuf[(int) currentStep % IO_SAMPLING_STEPS] = accFiles;
			long allTimeMaxAccessedFiles = Arrays.stream(maxAccessBuf).max().orElse(0L);

			// remove oldest value:
			decryptedBytesRead.getData().remove(0);
			encryptedBytesWrite.getData().remove(0);
			accessedFiles.getData().remove(0);

			// add latest value:
			decryptedBytesRead.getData().add(new Data<>(currentStep, decBytes));
			encryptedBytesWrite.getData().add(new Data<>(currentStep, encBytes));
			accessedFiles.getData().add(new Data<>(currentStep, accFiles));

			// adjust ranges:
			readChartXAxis.setLowerBound(currentStep - IO_SAMPLING_STEPS * 1.0);
			readChartXAxis.setUpperBound(currentStep);
			readChartYAxis.setUpperBound(allTimeMax);
			writeChartXAxis.setLowerBound(currentStep - IO_SAMPLING_STEPS * 1.0);
			writeChartXAxis.setUpperBound(currentStep);
			writeChartYAxis.setUpperBound(allTimeMax);
			accessChartXAxis.setLowerBound(currentStep - IO_SAMPLING_STEPS * 1.0);
			accessChartXAxis.setUpperBound(currentStep);
			accessChartYAxis.setUpperBound(allTimeMaxAccessedFiles);
		}
	}

	/* Getter/Setter */

	public LongBinding bpsReadProperty() {
		return bpsRead;
	}

	public long getBpsRead() {
		return bpsRead.get();
	}

	public LongBinding bpsWrittenProperty() {
		return bpsWritten;
	}

	public long getBpsWritten() {
		return bpsWritten.get();
	}

	public DoubleBinding cacheHitPercentageProperty() {
		return cacheHitPercentage;
	}

	public double getCacheHitPercentage() { return cacheHitPercentage.get(); }

	public DoubleBinding cacheHitDegreesProperty() {
		return cacheHitDegrees;
	}

	public double getCacheHitDegrees() {
		return cacheHitDegrees.get();
	}

	public LongBinding totalBytesReadProperty() { return totalBytesRead;}

	public long getTotalBytesRead() { return totalBytesRead.get();}

	public LongBinding totalBytesWrittenProperty() { return totalBytesWritten;}

	public long getTotalBytesWritten() { return totalBytesWritten.get();}

	public LongBinding totalBytesEncryptedProperty() {return totalBytesEncrypted;}

	public long getTotalBytesEncrypted() { return totalBytesEncrypted.get();}

	public LongBinding totalBytesDecryptedProperty() {return totalBytesDecrypted;}

	public long getTotalBytesDecrypted() { return totalBytesDecrypted.get();}

	public LongBinding bpsEncryptedProperty() {
		return bpsEncrypted;
	}

	public long getBpsEncrypted() {
		return bpsEncrypted.get();
	}

	public LongBinding bpsDecryptedProperty() {
		return bpsDecrypted;
	}

	public long getBpsDecrypted() {
		return bpsDecrypted.get();
	}

	public LongBinding filesReadProperty() { return filesRead;}

	public long getFilesRead() { return filesRead.get();}

	public LongBinding filesWrittenProperty() {return filesWritten;}

	public long getFilesWritten() {return filesWritten.get();}

	public LongBinding filesAccessedProperty() {return filesAccessed;}

	public long getFilesAccessed() {return filesAccessed.get();}

	public LongBinding totalFilesAccessedProperty() {return totalFilesAccessed;}

	public long getTotalFilesAccessed() {return totalFilesAccessed.get();}

	public LongBinding totalSizeProperty() { // Pfde9
		return totalSize;
	}

	public long getTotalSize() {
		return totalSize.get();
	}
}
