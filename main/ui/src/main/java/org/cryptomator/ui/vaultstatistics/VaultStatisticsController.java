package org.cryptomator.ui.vaultstatistics;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private static final int IO_SAMPLING_STEPS = 100;
	private static final double IO_SAMPLING_INTERVAL = 0.5;

	private final Stage window;
	private final Vault vault;
	@FXML
	private LineChart<Number, Number> lineGraph;
	private final Series<Number, Number> readData;
	private final Series<Number, Number> writeData;
	private Timeline ioAnimation;


	@Inject
	public VaultStatisticsController(@VaultStatisticsWindow Stage window, @VaultStatisticsWindow Vault vault) {
		this.window = window;
		this.vault = vault;

		readData = new Series<>();
		readData.setName("Read Data"); // For Legend
		//TODO Add Name to strings.properties
		writeData = new Series<>();
		writeData.setName("Write Data");
		//TODO Add Name to strings.properties

		ioAnimation = new Timeline(); //TODO Research better timer
		ioAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(IO_SAMPLING_INTERVAL), new IoSamplingAnimationHandler(readData, writeData)));
		ioAnimation.setCycleCount(Animation.INDEFINITE);
		ioAnimation.play();
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
			final long decBytes = vault.getStats().bytesPerSecondReadProperty().get();
			final double decMb = decBytes * BYTES_TO_MEGABYTES_FACTOR;
			final long encBytes = vault.getStats().bytesPerSecondWrittenProperty().get();
			final double encMb = encBytes * BYTES_TO_MEGABYTES_FACTOR;
			decryptedBytesRead.getData().get(IO_SAMPLING_STEPS - 1).setYValue(decMb);
			encryptedBytesWrite.getData().get(IO_SAMPLING_STEPS - 1).setYValue(encMb);
		}
	}

	public Vault getVault() {
		return vault;
	}
	/*
	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}*/
}
