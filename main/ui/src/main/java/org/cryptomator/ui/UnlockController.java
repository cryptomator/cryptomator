/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Directory;
import org.cryptomator.ui.util.MasterKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private ResourceBundle rb;
	private UnlockListener listener;
	private Directory directory;

	@FXML
	private ComboBox<String> usernameBox;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;

		usernameBox.valueProperty().addListener(this::didChooseUsername);
	}

	// ****************************************
	// Username box
	// ****************************************

	public void didChooseUsername(ObservableValue<? extends String> property, String oldValue, String newValue) {
		if (newValue != null) {
			Platform.runLater(passwordField::requestFocus);
		}
		passwordField.setDisable(StringUtils.isEmpty(newValue));
	}

	// ****************************************
	// Unlock button
	// ****************************************

	@FXML
	protected void didClickUnlockButton(ActionEvent event) {
		final String masterKeyFileName = usernameBox.getValue() + Aes256Cryptor.MASTERKEY_FILE_EXT;
		final Path masterKeyPath = directory.getPath().resolve(masterKeyFileName);
		final CharSequence password = passwordField.getCharacters();
		InputStream masterKeyInputStream = null;
		try {
			masterKeyInputStream = Files.newInputStream(masterKeyPath, StandardOpenOption.READ);
			directory.getCryptor().decryptMasterKey(masterKeyInputStream, password);
			if (!directory.startServer()) {
				messageLabel.setText(rb.getString("unlock.messageLabel.startServerFailed"));
				directory.getCryptor().swipeSensitiveData();
				return;
			}
			directory.setUnlocked(true);
			directory.mount();
			if (listener != null) {
				listener.didUnlock(this);
			}
		} catch (DecryptFailedException | IOException ex) {
			messageLabel.setText(rb.getString("unlock.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} catch (WrongPasswordException e) {
			messageLabel.setText(rb.getString("unlock.errorMessage.wrongPassword"));
		} catch (UnsupportedKeyLengthException ex) {
			messageLabel.setText(rb.getString("unlock.errorMessage.unsupportedKeyLengthInstallJCE"));
			LOG.warn("Unsupported Key-Length. Please install Oracle Java Cryptography Extension (JCE).", ex);
		} finally {
			passwordField.swipe();
			IOUtils.closeQuietly(masterKeyInputStream);
		}
	}

	private void findExistingUsernames() {
		try {
			DirectoryStream<Path> ds = MasterKeyFilter.filteredDirectory(directory.getPath());
			final String masterKeyExt = Aes256Cryptor.MASTERKEY_FILE_EXT.toLowerCase();
			usernameBox.getItems().clear();
			for (final Path path : ds) {
				final String fileName = path.getFileName().toString();
				final int beginOfExt = fileName.toLowerCase().lastIndexOf(masterKeyExt);
				final String baseName = fileName.substring(0, beginOfExt);
				usernameBox.getItems().add(baseName);
			}
			if (usernameBox.getItems().size() == 1) {
				usernameBox.getSelectionModel().selectFirst();
			}
		} catch (IOException e) {
			LOG.trace("Invalid path: " + directory.getPath(), e);
		}
	}

	/* Getter/Setter */

	public Directory getDirectory() {
		return directory;
	}

	public void setDirectory(Directory directory) {
		this.directory = directory;
		this.findExistingUsernames();
	}

	public UnlockListener getListener() {
		return listener;
	}

	public void setListener(UnlockListener listener) {
		this.listener = listener;
	}

	/* callback */

	interface UnlockListener {
		void didUnlock(UnlockController ctrl);
	}

}
