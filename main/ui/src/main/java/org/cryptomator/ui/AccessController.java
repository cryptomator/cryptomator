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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.WebDavMounter;
import org.cryptomator.ui.util.WebDavMounter.CommandFailedException;
import org.cryptomator.webdav.WebDAVServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

	private final Aes256Cryptor cryptor = new Aes256Cryptor();
	private final WebDAVServer server = new WebDAVServer();
	private ResourceBundle rb;
	private String unmountCmd;

	@FXML
	private GridPane rootPane;

	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;
	}

	@FXML
	protected void closeVault(ActionEvent event) {
		this.tryStop();
		this.rootPane.getScene().getWindow().hide();
	}

	public boolean unlockStorage(Path masterKeyPath, SecPasswordField passwordField, Label errorMessageLabel) {
		final CharSequence password = passwordField.getCharacters();
		InputStream masterKeyInputStream = null;
		try {
			masterKeyInputStream = Files.newInputStream(masterKeyPath, StandardOpenOption.READ);
			cryptor.decryptMasterKey(masterKeyInputStream, password);
			tryStart();
			return true;
		} catch (DecryptFailedException | IOException ex) {
			errorMessageLabel.setText(rb.getString("access.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} catch (WrongPasswordException e) {
			errorMessageLabel.setText(rb.getString("access.errorMessage.wrongPassword"));
		} catch (UnsupportedKeyLengthException ex) {
			errorMessageLabel.setText(rb.getString("access.errorMessage.unsupportedKeyLengthInstallJCE"));
			LOG.error("Unsupported Key-Length. Please install Oracle Java Cryptography Extension (JCE).", ex);
		} finally {
			passwordField.swipe();
			IOUtils.closeQuietly(masterKeyInputStream);
		}
		return false;
	}

	private void tryStart() {
		final Settings settings = Settings.load();
		final int webdavPort = server.start(settings.getWebdavWorkDir(), cryptor);
		if (webdavPort > 0) {
			try {
				unmountCmd = WebDavMounter.mount(webdavPort);
				MainApplication.addShutdownTask(this::tryStop);
			} catch (CommandFailedException e) {
				messageLabel.setText(String.format(rb.getString("access.messageLabel.mountFailed"), webdavPort));
				LOG.error("Mounting WebDAV share failed.", e);
			}
		}
	}

	public void tryStop() {
		if (server != null && server.isRunning()) {
			try {
				WebDavMounter.unmount(unmountCmd);
			} catch (CommandFailedException e) {
				LOG.warn("Unmounting WebDAV share failed.", e);
			}
			server.stop();
			cryptor.swipeSensitiveData();
		}
	}

}
