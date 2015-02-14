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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.FXThreads;
import org.cryptomator.ui.util.MasterKeyFilter;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.webdav.WebDavServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class UnlockController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private ResourceBundle rb;
	private UnlockListener listener;
	private Vault directory;

	@FXML
	private ComboBox<String> usernameBox;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private CheckBox checkIntegrity;

	@FXML
	private TextField mountName;

	@FXML
	private Button unlockButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label messageLabel;

	private final WebDavServer server;

	private final ExecutorService exec;

	private final DeferredCloser closer;

	@Inject
	public UnlockController(WebDavServer server, ExecutorService exec, DeferredCloser closer) {
		super();
		this.server = server;
		this.exec = exec;
		this.closer = closer;
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;

		usernameBox.valueProperty().addListener(this::didChooseUsername);
		mountName.textProperty().addListener(this::didTypeMountName);
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
	private void didClickUnlockButton(ActionEvent event) {
		setControlsDisabled(true);
		final String masterKeyFileName = usernameBox.getValue() + Aes256Cryptor.MASTERKEY_FILE_EXT;
		final Path masterKeyPath = directory.getPath().resolve(masterKeyFileName);
		final CharSequence password = passwordField.getCharacters();
		InputStream masterKeyInputStream = null;
		try {
			progressIndicator.setVisible(true);
			masterKeyInputStream = Files.newInputStream(masterKeyPath, StandardOpenOption.READ);
			directory.setVerifyFileIntegrity(checkIntegrity.isSelected());
			directory.getCryptor().decryptMasterKey(masterKeyInputStream, password);
			if (!directory.startServer(server, closer)) {
				messageLabel.setText(rb.getString("unlock.messageLabel.startServerFailed"));
				directory.getCryptor().swipeSensitiveData();
				return;
			}
			directory.setUnlocked(true);
			final Future<Boolean> futureMount = exec.submit(() -> directory.mount(closer));
			FXThreads.runOnMainThreadWhenFinished(exec, futureMount, this::didUnlockAndMount);
			FXThreads.runOnMainThreadWhenFinished(exec, futureMount, (result) -> {
				setControlsDisabled(false);
			});
		} catch (DecryptFailedException | IOException ex) {
			setControlsDisabled(false);
			progressIndicator.setVisible(false);
			messageLabel.setText(rb.getString("unlock.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} catch (WrongPasswordException e) {
			setControlsDisabled(false);
			progressIndicator.setVisible(false);
			messageLabel.setText(rb.getString("unlock.errorMessage.wrongPassword"));
			Platform.runLater(passwordField::requestFocus);
		} catch (UnsupportedKeyLengthException ex) {
			setControlsDisabled(false);
			progressIndicator.setVisible(false);
			messageLabel.setText(rb.getString("unlock.errorMessage.unsupportedKeyLengthInstallJCE"));
			LOG.warn("Unsupported Key-Length. Please install Oracle Java Cryptography Extension (JCE).", ex);
		} finally {
			passwordField.swipe();
			IOUtils.closeQuietly(masterKeyInputStream);
		}
	}

	private void setControlsDisabled(boolean disable) {
		usernameBox.setDisable(disable);
		passwordField.setDisable(disable);
		checkIntegrity.setDisable(disable);
		unlockButton.setDisable(disable);
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

	private void didUnlockAndMount(boolean mountSuccess) {
		progressIndicator.setVisible(false);
		if (listener != null) {
			listener.didUnlock(this);
		}
	}

	private void didTypeMountName(ObservableValue<? extends String> property, String oldValue, String newValue) {
		try {
			directory.setMountName(newValue);
			if (!newValue.equals(directory.getMountName())) {
				mountName.setText(directory.getMountName());
			}
		} catch (IllegalArgumentException e) {
			mountName.setText(directory.getMountName());
		}
	}

	/* Getter/Setter */

	public Vault getDirectory() {
		return directory;
	}

	public void setDirectory(Vault directory) {
		this.directory = directory;
		this.findExistingUsernames();
		this.checkIntegrity.setSelected(directory.shouldVerifyFileIntegrity());
		this.mountName.setText(directory.getMountName());
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
