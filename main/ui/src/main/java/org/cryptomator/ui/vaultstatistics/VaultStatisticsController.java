package org.cryptomator.ui.vaultstatistics;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.LongBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultStats;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.WeakBindings;

import javax.inject.Inject;

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private static final int IO_SAMPLING_STEPS = 100;
	private static final double IO_SAMPLING_INTERVAL = 0.5;

	private final VaultStats stats;
	private final Series<Number, Number> readData;
	private final Series<Number, Number> writeData;
	private final Timeline ioAnimation;
	private final LongBinding bpsRead;
	private final LongBinding bpsWritten;

	public LineChart<Number, Number> lineGraph;

	@Inject
	public VaultStatisticsController(@VaultStatisticsWindow Stage window, @VaultStatisticsWindow Vault vault) {
		this.stats = vault.getStats();
		this.bpsRead = WeakBindings.bindLong(stats.bytesPerSecondReadProperty());
		this.bpsWritten = WeakBindings.bindLong(stats.bytesPerSecondWrittenProperty());

		this.readData = new Series<>();
		readData.setName("Read Data"); // For Legend
		//TODO Add Name to strings.properties
		this.writeData = new Series<>();
		writeData.setName("Write Data");
		//TODO Add Name to strings.properties

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
		lineGraph.getData().addAll(readData, writeData);
	}

	private class IoSamplingAnimationHandler implements EventHandler<ActionEvent> {

		private static final double BYTES_TO_MEGABYTES_FACTOR = 1.0 / IO_SAMPLING_INTERVAL / 1024.0 / 1024.0;
		private final Series<Number, Number> decryptedBytesRead;
		private final Series<Number, Number> encryptedBytesWrite;

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
			// move all values one step:
			for (int i = 0; i < IO_SAMPLING_STEPS - 1; i++) {
				int j = i + 1;
				Number tmp = decryptedBytesRead.getData().get(j).getYValue();
				decryptedBytesRead.getData().get(i).setYValue(tmp);

				tmp = encryptedBytesWrite.getData().get(j).getYValue();
				encryptedBytesWrite.getData().get(i).setYValue(tmp);
			}

			// add latest value:
			final long decBytes = stats.bytesPerSecondReadProperty().get();
			final double decMb = decBytes * BYTES_TO_MEGABYTES_FACTOR;
			final long encBytes = stats.bytesPerSecondWrittenProperty().get();
			final double encMb = encBytes * BYTES_TO_MEGABYTES_FACTOR;
			decryptedBytesRead.getData().get(IO_SAMPLING_STEPS - 1).setYValue(decMb);
			encryptedBytesWrite.getData().get(IO_SAMPLING_STEPS - 1).setYValue(encMb);
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
}
