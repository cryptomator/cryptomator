package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.quit.QuitComponent;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

public class CustomDialogBuilder {

	public CustomDialogBuilder() {
	}

	public void showDialog(ResourceBundle resourceBundle, Stage owner, FontAwesome5Icon icon, String title, String message, String description, Runnable okAction, String okText) {
		try {
			FXMLLoader loader = new FXMLLoader(getResource(FxmlFile.CUSTOM_DIALOG), resourceBundle);
			Pane pane = loader.load();

			CustomDialogController controller = loader.getController();
			controller.setIcon(icon);
			controller.setMessage(message);
			controller.setDescription(description);
			controller.setOkButtonText(okText);
			controller.setOkAction(okAction);

			Stage dialogStage = new Stage();
			dialogStage.setTitle(title);
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(owner);
			dialogStage.setScene(new Scene(pane));
			dialogStage.setResizable(false);
			controller.setDialogStage(dialogStage);

			dialogStage.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();  // Handle loading errors
		}
	}

	private URL getResource(FxmlFile fxmlFile) {
		return getClass().getResource(fxmlFile.getRessourcePathString());
	}

}
