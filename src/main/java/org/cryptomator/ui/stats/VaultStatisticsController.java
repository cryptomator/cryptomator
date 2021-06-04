package org.cryptomator.ui.stats;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultStats;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.WeakBindings;

import javax.inject.Inject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.LongBinding;
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

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private static final int IO_SAMPLING_STEPS = 30;
	private static final double IO_SAMPLING_INTERVAL = 1;

	private final VaultStatisticsComponent component; // keep a strong reference to the component (see component's javadoc)
	private final VaultStats stats;
	private final Series<Number, Number> readData;
	private final Series<Number, Number> writeData;
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
	private final LongBinding bpsEncrypted;
	private final LongBinding bpsDecrypted;

	public AreaChart<Number, Number> readChart;
	public AreaChart<Number, Number> writeChart;
	public NumberAxis readChartXAxis;
	public NumberAxis readChartYAxis;
	public NumberAxis writeChartXAxis;
	public NumberAxis writeChartYAxis;

	@Inject
	public VaultStatisticsController(VaultStatisticsComponent component, @VaultStatisticsWindow Stage window, @VaultStatisticsWindow Vault vault) {
		this.component = component;
		this.stats = vault.getStats();
		this.readData = new Series<>();
		this.writeData = new Series<>();
		this.bpsRead = WeakBindings.bindLong(stats.bytesPerSecondReadProperty());
		this.bpsWritten = WeakBindings.bindLong(stats.bytesPerSecondWrittenProperty());
		this.cacheHitRate = WeakBindings.bindDouble(stats.cacheHitRateProperty());
		this.cacheHitDegrees = cacheHitRate.multiply(-270);
		this.cacheHitPercentage = cacheHitRate.multiply(100);
		this.totalBytesRead = WeakBindings.bindLong(stats.toalBytesReadProperty());
		this.totalBytesWritten = WeakBindings.bindLong(stats.toalBytesWrittenProperty());
		this.totalBytesDecrypted = WeakBindings.bindLong(stats.totalBytesDecryptedProperty());
		this.totalBytesEncrypted = WeakBindings.bindLong(stats.totalBytesEncryptedProperty());
		this.filesRead = WeakBindings.bindLong(stats.filesRead());
		this.filesWritten = WeakBindings.bindLong(stats.filesWritten());
		this.bpsEncrypted = WeakBindings.bindLong(stats.bytesPerSecondEncryptedProperty());
		this.bpsDecrypted = WeakBindings.bindLong(stats.bytesPerSecondDecryptedProperty());

		this.ioAnimation = new Timeline(); //TODO Research better timer
		ioAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(IO_SAMPLING_INTERVAL), new IoSamplingAnimationHandler(readData, writeData)));
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
	}

	private class IoSamplingAnimationHandler implements EventHandler<ActionEvent> {

		private long step = IO_SAMPLING_STEPS;
		private final Series<Number, Number> decryptedBytesRead;
		private final Series<Number, Number> encryptedBytesWrite;
		private final long[] maxBuf = new long[IO_SAMPLING_STEPS];

		public IoSamplingAnimationHandler(Series<Number, Number> readData, Series<Number, Number> writeData) {
			this.decryptedBytesRead = readData;
			this.encryptedBytesWrite = writeData;

			// initialize data once and change value of datapoints later:
			for (int i = 0; i < IO_SAMPLING_STEPS; i++) {
				decryptedBytesRead.getData().add(new Data<>(i, 0));
				encryptedBytesWrite.getData().add(new Data<>(i, 0));
			}
		}

		@Override
		public void handle(ActionEvent event) {
			final long currentStep = step++;
			final long decBytes = stats.bytesPerSecondReadProperty().get();
			final long encBytes = stats.bytesPerSecondWrittenProperty().get();

			maxBuf[(int) currentStep % IO_SAMPLING_STEPS] = Math.max(decBytes, encBytes);
			long allTimeMax = Arrays.stream(maxBuf).max().orElse(0l);

			// remove oldest value:
			decryptedBytesRead.getData().remove(0);
			encryptedBytesWrite.getData().remove(0);

			// add latest value:
			decryptedBytesRead.getData().add(new Data<>(currentStep, decBytes));
			encryptedBytesWrite.getData().add(new Data<>(currentStep, encBytes));

			// adjust ranges:
			readChartXAxis.setLowerBound(currentStep - IO_SAMPLING_STEPS);
			readChartXAxis.setUpperBound(currentStep);
			readChartYAxis.setUpperBound(allTimeMax);
			writeChartXAxis.setLowerBound(currentStep - IO_SAMPLING_STEPS);
			writeChartXAxis.setUpperBound(currentStep);
			writeChartYAxis.setUpperBound(allTimeMax);
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
}
