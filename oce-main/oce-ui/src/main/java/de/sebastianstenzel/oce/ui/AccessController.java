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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

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
import de.sebastianstenzel.oce.crypto.exceptions.DecryptFailedException;
import de.sebastianstenzel.oce.crypto.exceptions.UnsupportedKeyLengthException;
import de.sebastianstenzel.oce.crypto.exceptions.WrongPasswordException;
import de.sebastianstenzel.oce.ui.controls.SecPasswordField;
import de.sebastianstenzel.oce.ui.settings.Settings;
import de.sebastianstenzel.oce.webdav.WebDAVServer;

public class AccessController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

	private final Aes256Cryptor cryptor = new Aes256Cryptor();
	private ResourceBundle localization;
	@FXML
	private GridPane rootGridPane;
	@FXML
	private TextField workDirTextField;
	@FXML
	private SecPasswordField passwordField;
	@FXML
	private Button startServerButton;
	@FXML
	private Label messageLabel;

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
			final Path masterKeyPath = storagePath.resolve(Aes256Cryptor.MASTERKEY_FILENAME);
			storageLocationValid = Files.exists(masterKeyPath);
		} catch (InvalidPathException ex) {
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
			cryptor.swipeSensitiveData();
		} else if (this.unlockStorage()) {
			this.tryStart();
		}
	}

	private boolean unlockStorage() {
		final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
		final Path masterKeyPath = storagePath.resolve(Aes256Cryptor.MASTERKEY_FILENAME);
		final CharSequence password = passwordField.getCharacters();
		InputStream masterKeyInputStream = null;
		try {
			masterKeyInputStream = Files.newInputStream(masterKeyPath, StandardOpenOption.READ);
			cryptor.unlockStorage(masterKeyInputStream, password);
			return true;
		} catch (NoSuchFileException e) {
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
			IOUtils.closeQuietly(masterKeyInputStream);
		}
		return false;
	}

	private void tryStart() {
		try {
			final Settings settings = Settings.load();
			if (WebDAVServer.getInstance().start(settings.getWebdavWorkDir(), settings.getPort(), cryptor)) {
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
