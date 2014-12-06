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
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.controls.SecPasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializeController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private ResourceBundle localization;
	private SecPasswordField referencePasswordField;
	private Path masterKeyPath;
	private InitializationFinishedCallback callback;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button okButton;

	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.localization = rb;
		retypePasswordField.textProperty().addListener(this::retypePasswordFieldDidChange);
	}

	private void retypePasswordFieldDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		boolean passwordsAreEqual = referencePasswordField.getText().equals(retypePasswordField.getText());
		okButton.setDisable(!passwordsAreEqual);
	}

	@FXML
	protected void initWorkDir(ActionEvent event) {
		final Aes256Cryptor cryptor = new Aes256Cryptor();
		final CharSequence password = referencePasswordField.getCharacters();
		OutputStream masterKeyOutputStream = null;
		try {
			masterKeyOutputStream = Files.newOutputStream(masterKeyPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			cryptor.randomizeMasterKey();
			cryptor.encryptMasterKey(masterKeyOutputStream, password);
			cryptor.swipeSensitiveData();
			if (callback != null) {
				callback.initializationFinished(InitializationResult.SUCCESS);
			}
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (InvalidPathException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.invalidPath"));
		} catch (IOException ex) {
			LOG.error("I/O Exception", ex);
		} finally {
			retypePasswordField.swipe();
			IOUtils.closeQuietly(masterKeyOutputStream);
		}
	}

	@FXML
	protected void cancel(ActionEvent event) {
		if (callback != null) {
			callback.initializationFinished(InitializationResult.CANCELED);
		}
	}

	/* Getter/Setter */

	public SecPasswordField getReferencePasswordField() {
		return referencePasswordField;
	}

	public void setReferencePasswordField(SecPasswordField referencePasswordField) {
		this.referencePasswordField = referencePasswordField;
	}

	public Path getMasterKeyPath() {
		return masterKeyPath;
	}

	public void setMasterKeyPath(Path masterKeyPath) {
		this.masterKeyPath = masterKeyPath;
	}

	public InitializationFinishedCallback getCallback() {
		return callback;
	}

	public void setCallback(InitializationFinishedCallback callback) {
		this.callback = callback;
	}

	/* Modal callback stuff */

	enum InitializationResult {
		CANCELED, SUCCESS
	};

	interface InitializationFinishedCallback {
		void initializationFinished(InitializationResult result);
	}

}
