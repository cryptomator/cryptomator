package org.cryptomator.ui.decryptname;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javax.inject.Inject;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Path;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.ResourceBundle;

@DecryptNameScoped
public class DecryptFileNamesView implements FxController {

	private final ListProperty<Path> pathsToDecrypt;
	private final StringProperty dropZoneText = new SimpleStringProperty();
	private final ObjectProperty<FontAwesome5Icon> dropZoneIcon = new SimpleObjectProperty<>();
	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;

	@FXML
	public ListView<Path> decryptedNamesView;

	@Inject
	public DecryptFileNamesView(@DecryptNameWindow Stage window, @DecryptNameWindow Vault vault, @DecryptNameWindow List<Path> pathsToDecrypt, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
		this.pathsToDecrypt = new SimpleListProperty<>(FXCollections.observableArrayList(pathsToDecrypt));
	}

	@FXML
	public void initialize() {
		decryptedNamesView.setItems(pathsToDecrypt);
		//decryptedNamesView.setCellFactory(this::createListCell);
	}

	@FXML
	public void selectAndDecrypt() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.decryptName.filePickerTitle"));

		fileChooser.setInitialDirectory(vault.getPath().toFile());
		var ciphertextNodes = fileChooser.showOpenMultipleDialog(window);
		if (ciphertextNodes != null) {
			pathsToDecrypt.clear();
			pathsToDecrypt.addAll(ciphertextNodes.stream().map(File::toPath).toList());
		}
	}
	//obvservable getter

	public ObservableValue<String> dropZoneTextProperty() {
		return dropZoneText;
	}

	public String getDropZoneText() {
		return dropZoneText.get();
	}

	public ObservableValue<FontAwesome5Icon> dropZoneIconProperty() {
		return dropZoneIcon;
	}

	public FontAwesome5Icon getDropZoneIcon() {
		return dropZoneIcon.get();
	}

	public ObservableValue<Boolean> decryptedPathsListEmptyProperty() {
		return pathsToDecrypt.emptyProperty();
	}

	public boolean isDecryptedPathsListEmpty() {
		return pathsToDecrypt.isEmpty();
	}

}
