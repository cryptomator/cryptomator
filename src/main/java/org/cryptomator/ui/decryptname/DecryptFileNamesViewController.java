package org.cryptomator.ui.decryptname;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.common.Constants;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@DecryptNameScoped
public class DecryptFileNamesViewController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(DecryptFileNamesViewController.class);
	private static final KeyCodeCombination COPY_TO_CLIPBOARD_SHORTCUT = new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN);
	private static final String COPY_TO_CLIPBOARD_SHORTCUT_STRING_WIN = "CTRL+C";
	private static final String COPY_TO_CLIPBOARD_SHORTCUT_STRING_MAC = "⌘C";
	private static final String COPY_TO_CLIPBOARD_SHORTCUT_STRING_LINUX = "CTRL+C";

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
		//DragNDrop
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
		//selectionModel and copy-to-clipboard action
		cipherToCleartextTable.getSelectionModel().setCellSelectionEnabled(true);
		cipherToCleartextTable.setOnKeyPressed(keyEvent -> {
			if (COPY_TO_CLIPBOARD_SHORTCUT.match(keyEvent)) {
				copySingleCelltoClipboard();
			}
		});
		ciphertextColumn.setCellValueFactory(new PropertyValueFactory<>("ciphertextFilename"));
		cleartextColumn.setCellValueFactory(new PropertyValueFactory<>("cleartextName"));

		dropZoneText.setValue(resourceBundle.getString("decryptNames.dropZone.message"));
		dropZoneIcon.setValue(FontAwesome5Icon.FILE_IMPORT);

		wrongFilesSelected.addListener((_, _, areWrongFiles) -> {
			if (areWrongFiles) {
				CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, Platform::runLater).execute(() -> {
					dropZoneText.setValue(resourceBundle.getString("decryptNames.dropZone.message"));
					dropZoneIcon.setValue(FontAwesome5Icon.FILE_IMPORT);
					wrongFilesSelected.setValue(false);
				});
			}
		});
		if (!initialList.isEmpty()) {
			checkAndDecrypt(initialList);
		}
	}

	private void copySingleCelltoClipboard() {
		cipherToCleartextTable.getSelectionModel().getSelectedCells().stream().findFirst().ifPresent(tablePosition -> {
			var selectedItem = cipherToCleartextTable.getSelectionModel().getSelectedItem();
			//TODO: give user feedback, if content is copied -> must be done via a custom cell factory to access the actual table cell!
			if (tablePosition.getTableColumn().equals(ciphertextColumn)) {
				Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, selectedItem.ciphertext().toString()));
			} else {
				Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, selectedItem.cleartextName()));
			}
		});
	}

	@FXML
	public void selectFiles() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("decryptNames.filePicker.title"));
		fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(resourceBundle.getString("decryptNames.filePicker.extensionDescription"), List.of("*.c9r")));
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
			setDropZoneError(resourceBundle.getString("decryptNames.dropZone.error.foreignFiles").formatted(vault.getDisplayName()));
			return;
		}
		if (pathsToDecrypt.size() == 1 && testPath.endsWith(Constants.DIR_ID_BACKUP_FILE_NAME)) {
			setDropZoneError(resourceBundle.getString("decryptNames.dropZone.error.vaultInternalFiles"));
			return;
		}

		try {
			var newMapping = pathsToDecrypt.stream().filter(p -> !p.endsWith(Constants.DIR_ID_BACKUP_FILE_NAME)).map(this::getCleartextName).toList();
			mapping.addAll(newMapping);
		} catch (UncheckedIOException e) {
			setDropZoneError(resourceBundle.getString("decryptNames.dropZone.error.generic"));
			LOG.info("Failed to decrypt filenames for directory {}", testPath.getParent(), e);
		} catch (IllegalArgumentException e) {
			setDropZoneError(resourceBundle.getString("decryptNames.dropZone.error.vaultInternalFiles"));
		} catch (UnsupportedOperationException e) {
			setDropZoneError(resourceBundle.getString("decryptNames.dropZone.error.noDirIdBackup"));
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

	public void clearTable() {
		mapping.clear();
	}

	public void copyTableToClipboard() {
		var csv = mapping.stream().map(cipherAndClear -> "\"" + cipherAndClear.ciphertext() + "\", \"" + cipherAndClear.cleartextName() + "\"").collect(Collectors.joining("\n"));
		Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, csv));
	}

	public String getCopyToClipboardShortcutString() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return COPY_TO_CLIPBOARD_SHORTCUT_STRING_WIN;
		} else if (SystemUtils.IS_OS_MAC) {
			return COPY_TO_CLIPBOARD_SHORTCUT_STRING_MAC;
		} else {
			return COPY_TO_CLIPBOARD_SHORTCUT_STRING_LINUX;
		}
	}
}
