package org.cryptomator.ui.recoverykey;

import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import java.util.ResourceBundle;

public class RecoveryKeyDisplayController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyDisplayController.class);

	private final Stage window;
	private final String vaultName;
	private final String recoveryKey;
	private final ResourceBundle localization;
	public Button copyButton;

	public RecoveryKeyDisplayController(Stage window, String vaultName, String recoveryKey, ResourceBundle localization) {
		this.window = window;
		this.vaultName = vaultName;
		this.recoveryKey = recoveryKey;
		this.localization = localization;
	}

	@FXML
	public void printRecoveryKey() {
		PrinterJob job = PrinterJob.createPrinterJob();
		if (job != null && job.showPrintDialog(window)) {
			PageLayout pageLayout = job.getJobSettings().getPageLayout();

			String headingText = String.format(localization.getString("recoveryKey.printout.heading"), vaultName);
			Text heading = new Text(headingText);
			heading.setFont(Font.font("serif", FontWeight.BOLD, 20));
			heading.setFontSmoothingType(FontSmoothingType.LCD);

			Text key = new Text(recoveryKey);
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
		clipboardContent.putString(recoveryKey);
		Clipboard.getSystemClipboard().setContent(clipboardContent);
		LOG.info("Recovery key copied to clipboard.");

		copyButton.setText(localization.getString("generic.button.copied"));
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter/Setter */

	public boolean isPrinterSupported() {
		return Printer.getDefaultPrinter() != null;
	}

	public String getRecoveryKey() {
		return recoveryKey;
	}

	public String getVaultName() {
		return vaultName;
	}
}
