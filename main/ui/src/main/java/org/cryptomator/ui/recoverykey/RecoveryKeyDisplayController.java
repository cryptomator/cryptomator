package org.cryptomator.ui.recoverykey;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@RecoveryKeyScoped
public class RecoveryKeyDisplayController implements FxController {
	
	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyDisplayController.class);

	private final Stage window;
	private final Vault vault;
	private final StringProperty recoveryKeyProperty;
	private final ReadOnlyBooleanProperty printerSupported;

	@Inject
	public RecoveryKeyDisplayController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow StringProperty recoveryKey) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyProperty = recoveryKey;
		this.printerSupported = new SimpleBooleanProperty(Printer.getDefaultPrinter() != null);
	}

	@FXML
	public void printRecoveryKey() {
		// TODO localize

		PrinterJob job = PrinterJob.createPrinterJob();
		if (job != null && job.showPrintDialog(window)) {
			PageLayout pageLayout = job.getJobSettings().getPageLayout();

			Text heading = new Text("Cryptomator Recovery Key\n" + vault.getDisplayableName() + "\n");
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
	public void close() {
		window.close();
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public ReadOnlyBooleanProperty printerSupportedProperty() {
		return printerSupported;
	}

	public boolean isPrinterSupported() {
		return printerSupported.get();
	}

	public ReadOnlyStringProperty recoveryKeyProperty() {
		return recoveryKeyProperty;
	}

	public String getRecoveryKey() {
		return recoveryKeyProperty.get();
	}
}
