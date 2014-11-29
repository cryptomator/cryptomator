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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.MasterKeyFilter;
import org.cryptomator.ui.util.WebDavMounter;
import org.cryptomator.ui.util.WebDavMounter.CommandFailedException;
import org.cryptomator.webdav.WebDAVServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

	private final Aes256Cryptor cryptor = new Aes256Cryptor();
	private ResourceBundle localization;
	@FXML
	private GridPane rootGridPane;
	@FXML
	private TextField workDirTextField;
	@FXML
	private ComboBox<String> usernameBox;
	@FXML
	private SecPasswordField passwordField;
	@FXML
	private Button startServerButton;
	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.localization = rb;
		workDirTextField.textProperty().addListener(new WorkDirChangeListener());
		usernameBox.valueProperty().addListener(new UsernameChangeListener());
		workDirTextField.setText(Settings.load().getWebdavWorkDir());
		usernameBox.setValue(Settings.load().getUsername());
	}

	/**
	 * Step 1: Choose encrypted storage:
	 */
	@FXML
	protected void chooseWorkDir(ActionEvent event) {
		messageLabel.setText(null);
		final File currentFolder = new File(workDirTextField.getText());
		final DirectoryChooser dirChooser = new DirectoryChooser();
		if (currentFolder.exists()) {
			dirChooser.setInitialDirectory(currentFolder);
		}
		final File file = dirChooser.showDialog(rootGridPane.getScene().getWindow());
		if (file != null) {
			workDirTextField.setText(file.toString());
		}
	}

	private final class WorkDirChangeListener implements ChangeListener<String> {

		@Override
		public void changed(ObservableValue<? extends String> property, String oldValue, String newValue) {
			if (StringUtils.isEmpty(newValue)) {
				usernameBox.setDisable(true);
				usernameBox.setValue(null);
				return;
			}
			boolean storageLocationValid;
			try {
				final Path storagePath = FileSystems.getDefault().getPath(workDirTextField.getText());
				final DirectoryStream<Path> ds = MasterKeyFilter.filteredDirectory(storagePath);
				final String masterKeyExt = Aes256Cryptor.MASTERKEY_FILE_EXT.toLowerCase();
				usernameBox.getItems().clear();
				for (final Path path : ds) {
					final String fileName = path.getFileName().toString();
					final int beginOfExt = fileName.toLowerCase().lastIndexOf(masterKeyExt);
					final String baseName = fileName.substring(0, beginOfExt);
					usernameBox.getItems().add(baseName);
				}
				storageLocationValid = !usernameBox.getItems().isEmpty();
			} catch (InvalidPathException | IOException ex) {
				LOG.trace("Invalid path: " + workDirTextField.getText(), ex);
				storageLocationValid = false;
			}
			// valid encrypted folder?
			if (storageLocationValid) {
				Settings.load().setWebdavWorkDir(workDirTextField.getText());
				Settings.save();
			} else {
				messageLabel.setText(localization.getString("access.messageLabel.invalidStorageLocation"));
			}
			// enable/disable next controls:
			usernameBox.setDisable(!storageLocationValid);
			if (usernameBox.getItems().size() == 1) {
				usernameBox.setValue(usernameBox.getItems().get(0));
			}
		}

	}

	/**
	 * Step 2: Choose username
	 */
	private final class UsernameChangeListener implements ChangeListener<String> {
		@Override
		public void changed(ObservableValue<? extends String> property, String oldValue, String newValue) {
			if (newValue != null) {
				Settings.load().setUsername(newValue);
				Settings.save();
			}
			passwordField.setDisable(StringUtils.isEmpty(newValue));
			startServerButton.setDisable(StringUtils.isEmpty(newValue));
			Platform.runLater(passwordField::requestFocus);
		}
	}

	// step 3: Enter password

	/**
	 * Step 4: Unlock storage
	 */
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
		final String masterKeyFileName = usernameBox.getValue() + Aes256Cryptor.MASTERKEY_FILE_EXT;
		final Path masterKeyPath = storagePath.resolve(masterKeyFileName);
		final CharSequence password = passwordField.getCharacters();
		InputStream masterKeyInputStream = null;
		try {
			masterKeyInputStream = Files.newInputStream(masterKeyPath, StandardOpenOption.READ);
			cryptor.decryptMasterKey(masterKeyInputStream, password);
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
		final Settings settings = Settings.load();
		final int webdavPort = WebDAVServer.getInstance().start(settings.getWebdavWorkDir(), cryptor);
		if (webdavPort > 0) {
			startServerButton.setText(localization.getString("access.button.stopServer"));
			passwordField.setDisable(true);
			try {
				WebDavMounter.mount(webdavPort);
			} catch (CommandFailedException e) {
				messageLabel.setText(String.format(localization.getString("access.messageLabel.mountFailed"), webdavPort));
				LOG.error("Mounting WebDAV share failed.", e);
			}
		}
	}

	private void tryStop() {
		try {
			WebDavMounter.unmount(5);
			if (WebDAVServer.getInstance().stop()) {
				startServerButton.setText(localization.getString("access.button.startServer"));
				passwordField.setDisable(false);
			}
		} catch (CommandFailedException e) {
			LOG.warn("Unmounting WebDAV share failed.", e);
		}
	}

}
