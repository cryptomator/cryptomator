package org.cryptomator.ui.decryptname;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.common.Constants;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@DecryptNameScoped
public class DecryptFileNamesViewController implements FxController {

	private final ListProperty<CipherAndCleartext> mapping;
	private final StringProperty dropZoneText = new SimpleStringProperty();
	private final ObjectProperty<FontAwesome5Icon> dropZoneIcon = new SimpleObjectProperty<>();
	private final BooleanProperty wrongFilesSelected = new SimpleBooleanProperty(false);
	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;
	private final List<Path> initialList;

	@FXML
	public TableColumn<CipherAndCleartext, String> ciphertextColumn;
	@FXML
	public TableColumn<CipherAndCleartext, String> cleartextColumn;
	@FXML
	public TableView<CipherAndCleartext> cipherToCleartextTable;

	@Inject
	public DecryptFileNamesViewController(@DecryptNameWindow Stage window, @DecryptNameWindow Vault vault, @DecryptNameWindow List<Path> pathsToDecrypt, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
		this.mapping = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.initialList = pathsToDecrypt;
	}

	@FXML
	public void initialize() {
		cipherToCleartextTable.setItems(mapping);
		cipherToCleartextTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		cipherToCleartextTable.getSelectionModel().setCellSelectionEnabled(true);
		cipherToCleartextTable.setOnDragEntered(event -> {
			if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
				cipherToCleartextTable.setItems(FXCollections.emptyObservableList());
			}
		});
		cipherToCleartextTable.setOnDragOver(event -> {
			if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
				if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
					event.acceptTransferModes(TransferMode.LINK);
				} else {
					event.acceptTransferModes(TransferMode.ANY);
				}
			}
		});
		cipherToCleartextTable.setOnDragDropped(event -> {
			if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
				checkAndDecrypt(event.getDragboard().getFiles().stream().map(File::toPath).toList());
				cipherToCleartextTable.setItems(mapping);
			}
		});
		cipherToCleartextTable.setOnDragExited(_ -> cipherToCleartextTable.setItems(mapping));
		ciphertextColumn.setCellValueFactory(new PropertyValueFactory<>("ciphertextFilename"));
		cleartextColumn.setCellValueFactory(new PropertyValueFactory<>("cleartextName"));

		dropZoneText.setValue("Drop files or click to select");
		dropZoneIcon.setValue(FontAwesome5Icon.FILE_IMPORT);

		wrongFilesSelected.addListener((_, _, areWrongFiles) -> {
			if (areWrongFiles) {
				CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, Platform::runLater).execute(() -> {
					//dropZoneText.setValue(resourceBundle.getString(".."));
					dropZoneText.setValue("Drop files or click to select");
					dropZoneIcon.setValue(FontAwesome5Icon.FILE_IMPORT);
					wrongFilesSelected.setValue(false);
				});
			}
		});
		if (!initialList.isEmpty()) {
			checkAndDecrypt(initialList);
		}
	}

	@FXML
	public void selectFiles() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.decryptName.filePickerTitle"));
		fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Cryptomator encrypted files", List.of("*.c9r", "*.c9s")));
		fileChooser.setInitialDirectory(vault.getPath().toFile());
		var ciphertextNodes = fileChooser.showOpenMultipleDialog(window);
		if (ciphertextNodes != null) {
			checkAndDecrypt(ciphertextNodes.stream().map(File::toPath).toList());
		}
	}

	private void checkAndDecrypt(List<Path> pathsToDecrypt) {
		mapping.clear();
		//Assumption: All files are in the same directory
		var testPath = pathsToDecrypt.getFirst();
		if (!testPath.startsWith(vault.getPath())) {
			setDropZoneError("Selected files do not belong vault %s".formatted(vault.getDisplayName()));
			return;
		}
		if (pathsToDecrypt.size() == 1 && testPath.endsWith(Constants.DIR_ID_BACKUP_FILE_NAME)) {
			setDropZoneError("Vault internal files with no decrypt-able name selected");
			return;
		}

		try {
			var newMapping = pathsToDecrypt.stream().filter(p -> !p.endsWith(Constants.DIR_ID_BACKUP_FILE_NAME)).map(this::getCleartextName).toList();
			mapping.addAll(newMapping);
		} catch (UncheckedIOException e) {
			setDropZoneError("Failed to read selected files");
		} catch (IllegalArgumentException e) {
			setDropZoneError("Vault internal files with no decrypt-able name selected");
		}
	}

	private void setDropZoneError(String text) {
		dropZoneIcon.setValue(FontAwesome5Icon.TIMES);
		dropZoneText.setValue(text);
		wrongFilesSelected.setValue(true);
	}

	private CipherAndCleartext getCleartextName(Path ciphertextNode) {
		try {
			var cleartextName = vault.getCleartextName(ciphertextNode);
			return new CipherAndCleartext(ciphertextNode, cleartextName);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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

}
