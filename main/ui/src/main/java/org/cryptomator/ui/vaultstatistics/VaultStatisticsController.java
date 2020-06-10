package org.cryptomator.ui.vaultstatistics;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultStatisticsScoped
public class VaultStatisticsController implements FxController {

	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> vault;
	@FXML
	private LineChart<Double, Double> lineGraph;
	private final LongProperty currentReadData;
	private final LongProperty currentWriteData;
	private final XYChart.Series<Double, Double> readData;
	private final XYChart.Series<Double, Double> writeData;
	private long timeAtStartOfTracking;

	@Inject
	public VaultStatisticsController(@VaultStatisticsWindow Stage window, ObjectProperty<Vault> vault) {
		this.window = window;
		this.vault = vault;

		readData = new XYChart.Series<>();
		readData.setName("Read Data"); // For Legend
		//TODO Add Name to strings.properties
		writeData = new XYChart.Series<>();
		writeData.setName("Write Data");
		//TODO Add Name to strings.properties


		currentReadData = new SimpleLongProperty();
		currentReadData.bind(getVault().getStats().bytesPerSecondReadProperty());
		currentReadData.addListener((observable, oldValue, newValue) -> updateReadWriteData());

		currentWriteData = new SimpleLongProperty();
		currentWriteData.bind(getVault().getStats().bytesPerSecondWrittenProperty());
		currentWriteData.addListener((observable, oldValue, newValue) -> updateReadWriteData());
	}

	@FXML
	public void initialize() {
		window.setTitle(window.getTitle() + " - " + vault.get().getDisplayableName());
		lineGraph.getData().addAll(writeData, readData);
	}

	public Vault getVault() {
		return vault.get();
	}

	private void updateReadWriteData() {
		//So the graphs start at x = 0
		if (timeAtStartOfTracking == 0) {
			timeAtStartOfTracking = System.currentTimeMillis();
		}
		readData.getData().add(new XYChart.Data<Double, Double>((System.currentTimeMillis() - timeAtStartOfTracking) / 1000.0, ((getVault().getStats().bytesPerSecondReadProperty().get()) / 1024.0)));
		writeData.getData().add(new XYChart.Data<Double, Double>((System.currentTimeMillis() - timeAtStartOfTracking) / 1000.0, ((getVault().getStats().bytesPerSecondWrittenProperty().get()) / 1024.0)));
	}
}
