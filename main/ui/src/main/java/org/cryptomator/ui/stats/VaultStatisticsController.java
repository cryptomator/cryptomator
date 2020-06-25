package org.cryptomator.ui.stats;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.LongBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultStats;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.WeakBindings;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.ResourceBundle;

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private static final int IO_SAMPLING_STEPS = 30;
	private static final double IO_SAMPLING_INTERVAL = 1;

	private final VaultStats stats;
	private final Series<Number, Number> readData;
	private final Series<Number, Number> writeData;
	private final Timeline ioAnimation;
	private final LongBinding bpsRead;
	private final LongBinding bpsWritten;
	private final DoubleBinding cacheHitRate;
	private final DoubleBinding cacheHitDregrees;
	private final DoubleBinding cacheHitPercentage;

	public AreaChart<Number, Number> readChart;
	public AreaChart<Number, Number> writeChart;
	public NumberAxis readChartXAxis;
	public NumberAxis readChartYAxis;
	public NumberAxis writeChartXAxis;
	public NumberAxis writeChartYAxis;

	@Inject
	public VaultStatisticsController(@VaultStatisticsWindow Stage window, @VaultStatisticsWindow Vault vault, ResourceBundle resourceBundle) {
		this.stats = vault.getStats();
		this.readData = new Series<>();
		this.writeData = new Series<>();
		this.bpsRead = WeakBindings.bindLong(stats.bytesPerSecondReadProperty());
		this.bpsWritten = WeakBindings.bindLong(stats.bytesPerSecondWrittenProperty());
		this.cacheHitRate = WeakBindings.bindDouble(stats.cacheHitRateProperty());
		this.cacheHitDregrees = cacheHitRate.multiply(-270);
		this.cacheHitPercentage = cacheHitRate.multiply(100);

		this.ioAnimation = new Timeline(); //TODO Research better timer
		ioAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(IO_SAMPLING_INTERVAL), new IoSamplingAnimationHandler(readData, writeData)));
		ioAnimation.setCycleCount(Animation.INDEFINITE);
		ioAnimation.play();

		// make sure to stop animating,
		// otherwise a global timer (GC root) will keep a strong reference to animation
		window.setOnHiding(evt -> {
			ioAnimation.stop();
		});
	}

	@FXML
	public void initialize() {
		readChart.getData().addAll(readData);
		writeChart.getData().addAll(writeData);
	}

	private class IoSamplingAnimationHandler implements EventHandler<ActionEvent> {

		private static final double BYTES_TO_MEGABYTES_FACTOR = 1.0 / IO_SAMPLING_INTERVAL / 1024.0 / 1024.0;
		
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

	public double getCacheHitPercentage() {
		return cacheHitPercentage.get();
	}

	public DoubleBinding cacheHitDregreesProperty() {
		return cacheHitDregrees;
	}

	public double getCacheHitDregrees() {
		return cacheHitDregrees.get();
	}
}
