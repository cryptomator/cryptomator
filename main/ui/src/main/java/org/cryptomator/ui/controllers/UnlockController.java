/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.webdav.mount.WindowsDriveLetters;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.FXThreads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

public class UnlockController extends AbstractFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private Optional<UnlockListener> listener = Optional.empty();
	private Vault vault;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private TextField mountName;

	@FXML
	private Label winDriveLetterLabel;

	@FXML
	private ChoiceBox<Character> winDriveLetter;

	@FXML
	private Button advancedOptionsButton;

	@FXML
	private Button unlockButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Text messageText;

	@FXML
	private Hyperlink downloadsPageLink;

	@FXML
	private GridPane advancedOptions;

	private final ExecutorService exec;
	private final Application app;
	private final WindowsDriveLetters driveLetters;
	private final ChangeListener<Character> driveLetterChangeListener = this::winDriveLetterDidChange;

	@Inject
	public UnlockController(Application app, ExecutorService exec, WindowsDriveLetters driveLetters) {
		this.app = app;
		this.exec = exec;
		this.driveLetters = driveLetters;
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/unlock.fxml");
	}

	@Override
	protected ResourceBundle getFxmlResourceBundle() {
		return ResourceBundle.getBundle("localization");
	}

	@Override
	public void initialize() {
		passwordField.textProperty().addListener(this::passwordFieldsDidChange);
		advancedOptions.managedProperty().bind(advancedOptions.visibleProperty());
		mountName.addEventFilter(KeyEvent.KEY_TYPED, this::filterAlphanumericKeyEvents);
		mountName.textProperty().addListener(this::mountNameDidChange);
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.setConverter(new WinDriveLetterLabelConverter());
		} else {
			winDriveLetterLabel.setVisible(false);
			winDriveLetterLabel.setManaged(false);
			winDriveLetter.setVisible(false);
			winDriveLetter.setManaged(false);
		}
	}

	private void resetView() {
		passwordField.clear();
		unlockButton.setDisable(true);
		advancedOptions.setVisible(false);
		advancedOptionsButton.setText(resourceBundle.getString("unlock.button.advancedOptions.show"));
		progressIndicator.setVisible(false);
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.valueProperty().removeListener(driveLetterChangeListener);
			winDriveLetter.getItems().clear();
			winDriveLetter.getItems().add(null);
			winDriveLetter.getItems().addAll(driveLetters.getAvailableDriveLetters());
			winDriveLetter.getItems().sort(new WinDriveLetterComparator());
			winDriveLetter.valueProperty().addListener(driveLetterChangeListener);
		}
		downloadsPageLink.setVisible(false);
		messageText.setText(null);
	}

	// ****************************************
	// Password field
	// ****************************************

	private void passwordFieldsDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		boolean passwordIsEmpty = passwordField.getText().isEmpty();
		unlockButton.setDisable(passwordIsEmpty);
	}

	// ****************************************
	// Downloads link
	// ****************************************

	@FXML
	public void didClickDownloadsLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/downloads/");
	}

	// ****************************************
	// Advanced options button
	// ****************************************

	@FXML
	private void didClickAdvancedOptionsButton(ActionEvent event) {
		advancedOptions.setVisible(!advancedOptions.isVisible());
		if (advancedOptions.isVisible()) {
			advancedOptionsButton.setText(resourceBundle.getString("unlock.button.advancedOptions.hide"));
		} else {
			advancedOptionsButton.setText(resourceBundle.getString("unlock.button.advancedOptions.show"));
		}
	}

	private void filterAlphanumericKeyEvents(KeyEvent t) {
		if (t.getCharacter() == null || t.getCharacter().length() == 0) {
			return;
		}
		char c = CharUtils.toChar(t.getCharacter());
		if (!(CharUtils.isAsciiAlphanumeric(c) || c == '_')) {
			t.consume();
		}
	}

	private void mountNameDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		if (vault == null) {
			return;
		}
		// newValue is guaranteed to be a-z0-9_, see #filterAlphanumericKeyEvents
		if (newValue.isEmpty()) {
			mountName.setText(vault.getMountName());
		} else {
			vault.setMountName(newValue);
		}
	}

	/**
	 * Converts 'C' to "C:" to translate between model and GUI.
	 */
	private class WinDriveLetterLabelConverter extends StringConverter<Character> {

		@Override
		public String toString(Character letter) {
			if (letter == null) {
				return resourceBundle.getString("unlock.choicebox.winDriveLetter.auto");
			} else {
				return Character.toString(letter) + ":";
			}
		}

		@Override
		public Character fromString(String string) {
			if (resourceBundle.getString("unlock.choicebox.winDriveLetter.auto").equals(string)) {
				return null;
			} else {
				return CharUtils.toCharacterObject(string);
			}
		}

	}

	/**
	 * Natural sorting of ASCII letters, but <code>null</code> always on first, as this is "auto-assign".
	 */
	private static class WinDriveLetterComparator implements Comparator<Character> {

		@Override
		public int compare(Character c1, Character c2) {
			if (c1 == null) {
				return -1;
			} else if (c2 == null) {
				return 1;
			} else {
				return c1 - c2;
			}
		}
	}

	private void winDriveLetterDidChange(ObservableValue<? extends Character> property, Character oldValue, Character newValue) {
		if (vault == null) {
			return;
		}
		vault.setWinDriveLetter(newValue);
	}

	// ****************************************
	// Unlock button
	// ****************************************

	@FXML
	private void didClickUnlockButton(ActionEvent event) {
		setControlsDisabled(true);
		progressIndicator.setVisible(true);
		downloadsPageLink.setVisible(false);
		final CharSequence password = passwordField.getCharacters();
		try {
			vault.activateFrontend(password);
			Future<Boolean> futureMount = exec.submit(vault::mount);
			FXThreads.runOnMainThreadWhenFinished(exec, futureMount, this::unlockAndMountFinished);
		} catch (InvalidPassphraseException e) {
			setControlsDisabled(false);
			progressIndicator.setVisible(false);
			messageText.setText(resourceBundle.getString("unlock.errorMessage.wrongPassword"));
			Platform.runLater(passwordField::requestFocus);
		} catch (UnsupportedVaultFormatException e) {
			setControlsDisabled(false);
			progressIndicator.setVisible(false);
			downloadsPageLink.setVisible(true);
			LOG.warn("Unable to unlock vault: " + e.getMessage());
			if (e.isVaultOlderThanSoftware()) {
				messageText.setText(resourceBundle.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
			} else if (e.isSoftwareOlderThanVault()) {
				messageText.setText(resourceBundle.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
			}
		} catch (FrontendCreationFailedException ex) {
			setControlsDisabled(false);
			progressIndicator.setVisible(false);
			messageText.setText(resourceBundle.getString("unlock.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} finally {
			passwordField.swipe();
		}
	}

	private void setControlsDisabled(boolean disable) {
		passwordField.setDisable(disable);
		mountName.setDisable(disable);
		unlockButton.setDisable(disable);
		advancedOptionsButton.setDisable(disable);
	}

	private void unlockAndMountFinished(boolean mountSuccess) {
		progressIndicator.setVisible(false);
		setControlsDisabled(false);
		if (vault.isUnlocked() && !mountSuccess) {
			exec.submit(vault::deactivateFrontend);
		} else if (vault.isUnlocked() && mountSuccess) {
			exec.submit(() -> {
				try {
					vault.reveal();
				} catch (CommandFailedException e) {
					LOG.error("Failed to reveal mounted vault", e);
				}
			});
		}
		if (mountSuccess) {
			listener.ifPresent(this::invokeListenerLater);
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.resetView();
		this.vault = vault;
		this.mountName.setText(vault.getMountName());
		if (SystemUtils.IS_OS_WINDOWS) {
			chooseSelectedDriveLetter();
		}
	}

	private void chooseSelectedDriveLetter() {
		assert SystemUtils.IS_OS_WINDOWS;
		// if the vault prefers a drive letter, that is currently occupied, this is our last chance to reset this:
		if (driveLetters.getOccupiedDriveLetters().contains(vault.getWinDriveLetter())) {
			vault.setWinDriveLetter(null);
		}
		final Character letter = vault.getWinDriveLetter();
		if (letter == null) {
			// first option is known to be 'auto-assign' due to #WinDriveLetterComparator.
			this.winDriveLetter.getSelectionModel().selectFirst();
		} else {
			this.winDriveLetter.getSelectionModel().select(letter);
		}
	}

	public UnlockListener getListener() {
		return listener.orElse(null);
	}

	public void setListener(UnlockListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	/* callback */

	private void invokeListenerLater(UnlockListener listener) {
		Platform.runLater(() -> {
			listener.didUnlock(this);
		});
	}

	@FunctionalInterface
	interface UnlockListener {
		void didUnlock(UnlockController ctrl);
	}

}
