package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.FxmlFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class CustomDialogBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(CustomDialogBuilder.class);

	private String title;
	private String message;
	private String description;
	private FontAwesome5Icon icon;
	private String okButtonText = "OK";
	private String cancelButtonText = "Cancel";
	private Consumer<Stage> okAction;
	private Consumer<Stage> cancelAction;

	public CustomDialogBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public CustomDialogBuilder setMessage(String message) {
		this.message = message;
		return this;
	}

	public CustomDialogBuilder setDescription(String description) {
		this.description = description;
		return this;
	}

	public CustomDialogBuilder setIcon(FontAwesome5Icon icon) {
		this.icon = icon;
		return this;
	}

	public CustomDialogBuilder setOkButtonText(String okButtonText) {
		this.okButtonText = okButtonText;
		return this;
	}

	public CustomDialogBuilder setCancelButtonText(String cancelButtonText) {
		this.cancelButtonText = cancelButtonText;
		return this;
	}

	public CustomDialogBuilder setOkAction(Consumer<Stage> okAction) {
		this.okAction = okAction;
		return this;
	}

	public CustomDialogBuilder setCancelAction(Consumer<Stage> cancelAction) {
		this.cancelAction = cancelAction;
		return this;
	}


	public void buildAndShow(Stage owner) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(FxmlFile.CUSTOM_DIALOG.getRessourcePathString()));
			Pane pane = loader.load();

			CustomDialogController controller = loader.getController();
			controller.setMessage(message);
			controller.setDescription(description);
			controller.setIcon(icon);
			controller.setOkButtonText(okButtonText);
			controller.setCancelButtonText(cancelButtonText);

			Stage dialogStage = new Stage();
			dialogStage.setTitle(title);
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(owner);
			dialogStage.setScene(new Scene(pane));

			controller.setOkAction(() -> okAction.accept(dialogStage));
			controller.setCancelAction(() -> cancelAction.accept(dialogStage));
			dialogStage.showAndWait();
		} catch (Exception e) {
			LOG.error("Failed to build and show dialog stage.", e);
		}
	}
}