package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

public class CreateNewVaultRecoveryKeyController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CreateNewVaultRecoveryKeyController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final StringProperty recoveryKeyProperty;
	private final StringProperty vaultName;
	private final ReadOnlyBooleanProperty printerSupported;
	public TextArea textarea;

	@Inject
	CreateNewVaultRecoveryKeyController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, @Named("recoveryKey") StringProperty recoveryKey, @Named("vaultName") StringProperty vaultName) {
		this.window = window;
		this.successScene = successScene;
		this.recoveryKeyProperty = recoveryKey;
		this.vaultName = vaultName;
		this.printerSupported = new SimpleBooleanProperty(Printer.getDefaultPrinter() != null);
	}

	@FXML
	public void printRecoveryKey() {
		// TODO localize
		
		PrinterJob job = PrinterJob.createPrinterJob();
		if (job != null && job.showPrintDialog(window)) {
			PageLayout pageLayout = job.getJobSettings().getPageLayout();

			Text heading = new Text("Cryptomator Recovery Key\n" + vaultName.get() + "\n");
			heading.setFont(Font.font("serif", FontWeight.BOLD, 20));
			heading.setFontSmoothingType(FontSmoothingType.LCD);

			Text key = new Text(recoveryKeyProperty.get());
			key.setFont(Font.font("serif", FontWeight.NORMAL, 16));
			key.setFontSmoothingType(FontSmoothingType.GRAY);

			TextFlow textFlow = new TextFlow();
			textFlow.setPrefSize(pageLayout.getPrintableWidth(), pageLayout.getPrintableHeight());
			textFlow.getChildren().addAll(heading, key);
			textFlow.setLineSpacing(6);

			if (job.printPage(textFlow)) {
				LOG.info("Recovery key printed.");
				job.endJob();
			} else {
				LOG.warn("Printing recovery key failed.");
			}
		} else {
			LOG.info("Printing recovery key canceled by user.");
		}
	}

	@FXML
	public void copyRecoveryKey() {
		ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(recoveryKeyProperty.get());
		Clipboard.getSystemClipboard().setContent(clipboardContent);
		LOG.info("Recovery key copied to clipboard.");
	}

	@FXML
	public void next() {
		window.setScene(successScene.get());
	}

	/* Getter/Setter */

	public ReadOnlyBooleanProperty printerSupportedProperty() {
		return printerSupported;
	}

	public boolean isPrinterSupported() {
		return printerSupported.get();
	}

	public String getRecoveryKey() {
		return recoveryKeyProperty.get();
	}

	public StringProperty recoveryKeyProperty() {
		return recoveryKeyProperty;
	}
}
