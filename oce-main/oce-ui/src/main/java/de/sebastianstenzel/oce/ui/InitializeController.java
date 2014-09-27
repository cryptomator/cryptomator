/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.sebastianstenzel.oce.crypto.aes256.Aes256Cryptor;
import de.sebastianstenzel.oce.ui.controls.SecPasswordField;

public class InitializeController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private ResourceBundle localization;
	@FXML
	private GridPane rootGridPane;
	@FXML
	private TextField workDirTextField;
	@FXML
	private SecPasswordField passwordField;
	@FXML
	private SecPasswordField retypePasswordField;
	@FXML
	private Button initWorkDirButton;
	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.localization = rb;
		passwordField.textProperty().addListener(new PasswordChangeListener());
		retypePasswordField.textProperty().addListener(new RetypePasswordChangeListener());
	}

	/**
	 * Step 1: Choose a directory, that shall be encrypted. On success, step 2 will be enabled.
	 */
	@FXML
	protected void chooseWorkDir(ActionEvent event) {
		final File currentFolder = new File(workDirTextField.getText());
		final DirectoryChooser dirChooser = new DirectoryChooser();
		if (currentFolder.exists()) {
			dirChooser.setInitialDirectory(currentFolder);
		}
		final File file = dirChooser.showDialog(rootGridPane.getScene().getWindow());
		if (file != null && file.canWrite()) {
			workDirTextField.setText(file.getPath());
			passwordField.setDisable(false);
			passwordField.selectAll();
			passwordField.requestFocus();
		}
	}

	/**
	 * Step 2: Defina a password. On success, step 3 will be enabled.
	 */
	private final class PasswordChangeListener implements ChangeListener<String> {
		@Override
		public void changed(ObservableValue<? extends String> property, String oldValue, String newValue) {
			retypePasswordField.setDisable(newValue.isEmpty());
		}
	}

	/**
	 * Step 3: Retype the password. On success, step 4 will be enabled.
	 */
	private final class RetypePasswordChangeListener implements ChangeListener<String> {
		@Override
		public void changed(ObservableValue<? extends String> property, String oldValue, String newValue) {
			boolean passwordsAreEqual = passwordField.getText().equals(newValue);
			initWorkDirButton.setDisable(!passwordsAreEqual);
		}
	}

	/**
	 * Step 4: Generate master password file in working directory. On success, print success message.
	 */
	@FXML
	protected void initWorkDir(ActionEvent event) {
		final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
		final Path masterKeyPath = storagePath.resolve(Aes256Cryptor.MASTERKEY_FILENAME);
		final Aes256Cryptor cryptor = new Aes256Cryptor();
		final CharSequence password = passwordField.getCharacters();
		OutputStream masterKeyOutputStream = null;
		try {
			masterKeyOutputStream = Files.newOutputStream(masterKeyPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			cryptor.initializeStorage(masterKeyOutputStream, password);
			cryptor.swipeSensitiveData();
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (InvalidPathException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.invalidPath"));
		} catch (IOException ex) {
			LOG.error("I/O Exception", ex);
		} finally {
			swipePasswordFields();
			IOUtils.closeQuietly(masterKeyOutputStream);
		}
	}

	private void swipePasswordFields() {
		passwordField.swipe();
		retypePasswordField.swipe();
	}

}
