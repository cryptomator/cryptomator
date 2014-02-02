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
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.crypto.StorageCrypting.DecryptFailedException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.InvalidStorageLocationException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.UnsupportedKeyLengthException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.WrongPasswordException;
import de.sebastianstenzel.oce.ui.controls.SecPasswordField;
import de.sebastianstenzel.oce.ui.settings.Settings;
import de.sebastianstenzel.oce.webdav.WebDAVServer;

public class AccessController implements Initializable {
	
	private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);
	
	private ResourceBundle localization;
	@FXML private GridPane rootGridPane;
	@FXML private TextField workDirTextField;
	@FXML private SecPasswordField passwordField;
	@FXML private Button startServerButton;
	@FXML private Label messageLabel;
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.localization = rb;
		workDirTextField.setText(Settings.load().getWebdavWorkDir());
		determineStorageValidity();
	}
	
	@FXML
	protected void chooseWorkDir(ActionEvent event) {
		messageLabel.setText(null);
		final File currentFolder = new File(workDirTextField.getText());
		final DirectoryChooser dirChooser = new DirectoryChooser();
		if (currentFolder.exists()) {
			dirChooser.setInitialDirectory(currentFolder);
		}
		final File file = dirChooser.showDialog(rootGridPane.getScene().getWindow());
		if (file == null) {
			// dialog canceled
			return;
		} else if (file.canWrite()) {
			workDirTextField.setText(file.getPath());
			Settings.load().setWebdavWorkDir(file.getPath());
			Settings.save();
		} else {
			messageLabel.setText(localization.getString("access.messageLabel.invalidStorageLocation"));
		}
		determineStorageValidity();
	}
	
	private void determineStorageValidity() {
		boolean storageLocationValid;
		try {
			final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
			storageLocationValid = Cryptor.getDefaultCryptor().isStorage(storagePath);
		} catch(InvalidPathException ex) {
			LOG.trace("Invalid path: " + workDirTextField.getText(), ex);
			storageLocationValid = false;
		}
		passwordField.setDisable(!storageLocationValid);
		startServerButton.setDisable(!storageLocationValid);
	}
	
	@FXML
	protected void startStopServer(ActionEvent event) {
		messageLabel.setText(null);
		if (WebDAVServer.getInstance().isRunning()) {
			this.tryStop();
			Cryptor.getDefaultCryptor().swipeSensitiveData();
		} else if (this.unlockStorage()) {
			this.tryStart();
		}
	}
	
	private boolean unlockStorage() {
		final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
		final CharSequence password = passwordField.getCharacters();
		try {
			Cryptor.getDefaultCryptor().unlockStorage(storagePath, password);
			return true;
		} catch (InvalidStorageLocationException e) {
			messageLabel.setText(localization.getString("access.messageLabel.invalidStorageLocation"));
			LOG.warn("Invalid path: " + storagePath.toString());
		} catch (DecryptFailedException ex) {
			messageLabel.setText(localization.getString("access.messageLabel.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} catch (WrongPasswordException e) {
			messageLabel.setText(localization.getString("access.messageLabel.wrongPassword"));
		} catch (UnsupportedKeyLengthException ex) {
			messageLabel.setText(localization.getString("access.messageLabel.unsupportedKeyLengthInstallJCE"));
			LOG.error("Unsupported Key-Length. Please install Oracle Java Cryptography Extension (JCE).", ex);
		} catch (IOException ex) {
			LOG.error("I/O Exception", ex);
		} finally {
			passwordField.swipe();
		}
		return false;
	}
	
	private void tryStart() {
		try {
			final Settings settings = Settings.load();
			if (WebDAVServer.getInstance().start(settings.getWebdavWorkDir(), settings.getPort())) {
				startServerButton.setText(localization.getString("access.button.stopServer"));
				passwordField.setDisable(true);
			}
		} catch (NumberFormatException ex) {
			LOG.error("Invalid port", ex);
		}
	}
	
	private void tryStop() {
		if (WebDAVServer.getInstance().stop()) {
			startServerButton.setText(localization.getString("access.button.startServer"));
			passwordField.setDisable(false);
		}
	}

}
