/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.InitializeController.InitializationResult;
import org.cryptomator.ui.controls.ClearOnDisableListener;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.MasterKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
	private static final int MAX_USERNAME_LENGTH = 200;

	private ResourceBundle rb;

	private Workflow workflow = Workflow.UNKNOWN;

	private enum Workflow {
		UNKNOWN, INIT, OPEN
	};

	@FXML
	private GridPane rootPane;

	@FXML
	private TextField workDirTextField;

	@FXML
	private TextField usernameField;

	@FXML
	private ComboBox<String> usernameBox;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private SplitMenuButton openButton;

	@FXML
	private Button initializeButton;

	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;
		// attach event handler
		workDirTextField.textProperty().addListener(this::workDirDidChange);
		usernameField.addEventFilter(KeyEvent.KEY_TYPED, this::filterUsernameKeyEvents);
		usernameField.disableProperty().addListener(new ClearOnDisableListener(usernameField));
		usernameField.textProperty().addListener(this::usernameFieldDidChange);
		usernameBox.valueProperty().addListener(this::usernameBoxDidChange);
		passwordField.disableProperty().addListener(new ClearOnDisableListener(passwordField));
		passwordField.textProperty().addListener(this::passwordFieldDidChange);
		passwordField.addEventHandler(KeyEvent.KEY_PRESSED, this::onPasswordFieldKeyPressed);

		// load settings
		workDirTextField.setText(Settings.load().getWebdavWorkDir());
		usernameBox.setValue(Settings.load().getUsername());
	}

	// ****************************************
	// Workdir field
	// ****************************************

	@FXML
	protected void chooseWorkDir(ActionEvent event) {
		final File currentFolder = new File(workDirTextField.getText());
		final DirectoryChooser dirChooser = new DirectoryChooser();
		if (currentFolder.exists()) {
			dirChooser.setInitialDirectory(currentFolder);
		}
		final File file = dirChooser.showDialog(rootPane.getScene().getWindow());
		if (file != null && file.canWrite()) {
			workDirTextField.setText(file.toString());
		}
	}

	private void workDirDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		if (StringUtils.isEmpty(newValue)) {
			usernameField.setDisable(true);
			usernameBox.setDisable(true);
			return;
		}
		try {
			final Path dir = FileSystems.getDefault().getPath(newValue);
			final Iterator<Path> masterKeys = MasterKeyFilter.filteredDirectory(dir).iterator();
			if (masterKeys.hasNext()) {
				workflow = Workflow.OPEN;
				showUsernameBox(masterKeys);
				showOpenButton();
			} else {
				workflow = Workflow.INIT;
				showUsernameField();
				showInitializeButton();
			}
			usernameField.setDisable(false);
			usernameBox.setDisable(false);
			Settings.load().setWebdavWorkDir(newValue);
		} catch (InvalidPathException | IOException e) {
			usernameField.setDisable(true);
			usernameBox.setDisable(true);
			messageLabel.setText(rb.getString("main.messageLabel.invalidPath"));
		}
	}

	// ****************************************
	// Username field
	// ****************************************

	private void showUsernameField() {
		messageLabel.setText(rb.getString("main.messageLabel.initVaultMessage"));
		usernameBox.setVisible(false);
		usernameField.setVisible(true);
		Platform.runLater(usernameField::requestFocus);
	}

	private void usernameFieldDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		if (StringUtils.length(newValue) > MAX_USERNAME_LENGTH) {
			usernameField.setText(newValue.substring(0, MAX_USERNAME_LENGTH));
		}
		passwordField.setDisable(StringUtils.isEmpty(usernameField.getText()));
	}

	private void filterUsernameKeyEvents(KeyEvent t) {
		if (t.getCharacter() == null || t.getCharacter().length() == 0) {
			return;
		}
		char c = t.getCharacter().charAt(0);
		if (!CharUtils.isAsciiAlphanumeric(c)) {
			t.consume();
		}
	}

	// ****************************************
	// Username box
	// ****************************************

	private void showUsernameBox(Iterator<Path> foundMasterKeys) {
		messageLabel.setText(rb.getString("main.messageLabel.openVaultMessage"));
		usernameField.setVisible(false);
		usernameBox.setVisible(true);

		// update usernameBox options:
		usernameBox.getItems().clear();
		final String masterKeyExt = Aes256Cryptor.MASTERKEY_FILE_EXT.toLowerCase();
		foundMasterKeys.forEachRemaining(path -> {
			final String fileName = path.getFileName().toString();
			final int beginOfExt = fileName.toLowerCase().lastIndexOf(masterKeyExt);
			final String baseName = fileName.substring(0, beginOfExt);
			usernameBox.getItems().add(baseName);
		});

		// autochoose user, if possible:
		if (usernameBox.getItems().size() == 1) {
			usernameBox.setValue(usernameBox.getItems().get(0));
		}
	}

	private void usernameBoxDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		if (!Workflow.OPEN.equals(workflow)) {
			return;
		}
		if (newValue != null) {
			Settings.load().setUsername(newValue);
		}
		passwordField.setDisable(StringUtils.isEmpty(newValue));
		Platform.runLater(passwordField::requestFocus);
	}

	// ****************************************
	// Password field
	// ****************************************

	private void passwordFieldDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		initializeButton.setDisable(StringUtils.isEmpty(newValue));
		openButton.setDisable(StringUtils.isEmpty(newValue));
	}

	public void onPasswordFieldKeyPressed(KeyEvent event) {
		if (KeyCode.ENTER.equals(event.getCode())) {
			switch (workflow) {
			case OPEN:
				openButton.fire();
				break;
			case INIT:
				initializeButton.fire();
				break;
			default:
				break;
			}
		}
	}

	// ****************************************
	// Initialize vault button
	// ****************************************

	private void showInitializeButton() {
		openButton.setVisible(false);
		initializeButton.setVisible(true);
	}

	@FXML
	protected void showInitializationDialog(ActionEvent event) {
		final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
		final String masterKeyFileName = usernameField.getText() + Aes256Cryptor.MASTERKEY_FILE_EXT;
		final Path masterKeyPath = storagePath.resolve(masterKeyFileName);

		try {
			final FXMLLoader loader = new FXMLLoader(getClass().getResource("/initialize.fxml"), rb);
			final Parent initDialog = loader.load();
			final Scene dialogScene = new Scene(initDialog);
			final Stage dialog = new Stage();
			final InitializeController ctrl = loader.getController();
			ctrl.setReferencePasswordField(passwordField);
			ctrl.setMasterKeyPath(masterKeyPath);
			ctrl.setCallback(result -> {
				if (InitializationResult.SUCCESS.equals(result)) {
					this.initializationSucceeded();
				}
				dialog.close();
			});
			dialog.initModality(Modality.APPLICATION_MODAL);
			dialog.initOwner(rootPane.getScene().getWindow());
			dialog.setTitle(rb.getString("initialize.title"));
			dialog.setScene(dialogScene);
			dialog.sizeToScene();
			dialog.setResizable(false);
			dialog.show();
		} catch (IOException e) {
			LOG.error("Failed to load fxml file.", e);
		}
	}

	private void initializationSucceeded() {
		// trigger re-evaluation of work dir. there should be a masterkey file now.
		this.workDirDidChange(workDirTextField.textProperty(), workDirTextField.getText(), workDirTextField.getText());
	}

	// ****************************************
	// Open vault button
	// ****************************************

	private void showOpenButton() {
		initializeButton.setVisible(false);
		openButton.setVisible(true);
	}

	@FXML
	protected void openVault(ActionEvent event) {
		final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
		final String masterKeyFileName = usernameBox.getValue() + Aes256Cryptor.MASTERKEY_FILE_EXT;
		final Path masterKeyPath = storagePath.resolve(masterKeyFileName);

		try {
			final FXMLLoader loader = new FXMLLoader(getClass().getResource("/access.fxml"), rb);
			final Parent accessDialog = loader.load();
			final Scene dialogScene = new Scene(accessDialog);
			final AccessController ctrl = loader.getController();
			if (ctrl.unlockStorage(masterKeyPath, passwordField, messageLabel)) {
				passwordField.swipe();
				final Stage dialog = new Stage();
				dialog.initModality(Modality.NONE);
				dialog.initOwner(rootPane.getScene().getWindow());
				dialog.setTitle(storagePath.getFileName().toString());
				dialog.setScene(dialogScene);
				dialog.sizeToScene();
				dialog.setResizable(false);
				dialog.show();
				dialog.setOnCloseRequest(windowEvent -> {
					ctrl.tryStop();
				});
			} else {
				Platform.runLater(passwordField::requestFocus);
			}
		} catch (IOException e) {
			LOG.error("Failed to load fxml file.", e);
		}
	}
}
