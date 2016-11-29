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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.frontend.webdav.mount.WindowsDriveLetters;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.AsyncTaskService;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dagger.Lazy;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

public class UnlockController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private final Application app;
	private final AsyncTaskService asyncTaskService;
	private final Lazy<FrontendFactory> frontendFactory;
	private final Settings settings;
	private final WindowsDriveLetters driveLetters;
	private final ChangeListener<Character> driveLetterChangeListener = this::winDriveLetterDidChange;
	private final Optional<KeychainAccess> keychainAccess;
	private Vault vault;
	private Optional<UnlockListener> listener = Optional.empty();

	@Inject
	public UnlockController(Application app, Localization localization, AsyncTaskService asyncTaskService, Lazy<FrontendFactory> frontendFactory, Settings settings, WindowsDriveLetters driveLetters,
			Optional<KeychainAccess> keychainAccess) {
		super(localization);
		this.app = app;
		this.asyncTaskService = asyncTaskService;
		this.frontendFactory = frontendFactory;
		this.settings = settings;
		this.driveLetters = driveLetters;
		this.keychainAccess = keychainAccess;
	}

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private Button advancedOptionsButton;

	@FXML
	private Button unlockButton;

	@FXML
	private CheckBox savePassword;

	@FXML
	private TextField mountName;

	@FXML
	private Label winDriveLetterLabel;

	@FXML
	private ChoiceBox<Character> winDriveLetter;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Text messageText;

	@FXML
	private Hyperlink downloadsPageLink;

	@FXML
	private GridPane advancedOptions;

	@Override
	public void initialize() {
		advancedOptions.managedProperty().bind(advancedOptions.visibleProperty());
		unlockButton.disableProperty().bind(passwordField.textProperty().isEmpty());
		mountName.addEventFilter(KeyEvent.KEY_TYPED, this::filterAlphanumericKeyEvents);
		mountName.textProperty().addListener(this::mountNameDidChange);
		savePassword.setDisable(!keychainAccess.isPresent());
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.setConverter(new WinDriveLetterLabelConverter());
		} else {
			winDriveLetterLabel.setVisible(false);
			winDriveLetterLabel.setManaged(false);
			winDriveLetter.setVisible(false);
			winDriveLetter.setManaged(false);
		}
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/unlock.fxml");
	}

	void setVault(Vault vault) {
		// trigger "default" change to refresh key bindings:
		unlockButton.setDefaultButton(false);
		unlockButton.setDefaultButton(true);
		if (vault.equals(this.vault)) {
			return;
		}
		this.vault = Objects.requireNonNull(vault);
		passwordField.swipe();
		advancedOptions.setVisible(false);
		advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
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
		mountName.setText(vault.getMountName());
		if (SystemUtils.IS_OS_WINDOWS) {
			chooseSelectedDriveLetter();
		}
		savePassword.setSelected(false);
		// auto-fill pw from keychain:
		if (keychainAccess.isPresent()) {
			char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId());
			if (storedPw != null) {
				savePassword.setSelected(true);
				passwordField.setText(new String(storedPw));
				passwordField.selectRange(storedPw.length, storedPw.length);
				Arrays.fill(storedPw, ' ');
			}
		}
	}

	// ****************************************
	// Downloads link
	// ****************************************

	@FXML
	public void didClickDownloadsLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/downloads/#allVersions");
	}

	// ****************************************
	// Advanced options button
	// ****************************************

	@FXML
	private void didClickAdvancedOptionsButton(ActionEvent event) {
		advancedOptions.setVisible(!advancedOptions.isVisible());
		if (advancedOptions.isVisible()) {
			advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.hide"));
		} else {
			advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
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
				return localization.getString("unlock.choicebox.winDriveLetter.auto");
			} else {
				return Character.toString(letter) + ":";
			}
		}

		@Override
		public Character fromString(String string) {
			if (localization.getString("unlock.choicebox.winDriveLetter.auto").equals(string)) {
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
		vault.setWinDriveLetter(newValue);
		settings.save();
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

	// ****************************************
	// Save password checkbox
	// ****************************************

	@FXML
	private void didClickSavePasswordCheckbox(ActionEvent event) {
		if (!savePassword.isSelected() && hasStoredPassword()) {
			Alert confirmDialog = DialogBuilderUtil.buildConfirmationDialog( //
					localization.getString("unlock.savePassword.delete.confirmation.title"), //
					localization.getString("unlock.savePassword.delete.confirmation.header"), //
					localization.getString("unlock.savePassword.delete.confirmation.content"), //
					SystemUtils.IS_OS_MAC_OSX ? ButtonType.CANCEL : ButtonType.OK);

			Optional<ButtonType> choice = confirmDialog.showAndWait();
			if (ButtonType.OK.equals(choice.get())) {
				keychainAccess.get().deletePassphrase(vault.getId());
			} else if (ButtonType.CANCEL.equals(choice.get())) {
				savePassword.setSelected(true);
			}
		}
	}

	private boolean hasStoredPassword() {
		char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId());
		boolean hasPw = (storedPw != null);
		if (storedPw != null) {
			Arrays.fill(storedPw, ' ');
		}
		return hasPw;
	}

	// ****************************************
	// Unlock button
	// ****************************************

	@FXML
	private void didClickUnlockButton(ActionEvent event) {
		mountName.setDisable(true);
		advancedOptionsButton.setDisable(true);
		progressIndicator.setVisible(true);
		downloadsPageLink.setVisible(false);
		CharSequence password = passwordField.getCharacters();
		asyncTaskService.asyncTaskOf(() -> this.unlock(password)).run();
	}

	private void unlock(CharSequence password) {
		try {
			vault.activateFrontend(frontendFactory.get(), settings, password);
			vault.reveal();
			Platform.runLater(() -> {
				messageText.setText(null);
				listener.ifPresent(lstnr -> lstnr.didUnlock(vault));
			});
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				keychainAccess.get().storePassphrase(vault.getId(), password);
			} else {
				Platform.runLater(passwordField::swipe);
			}
		} catch (InvalidPassphraseException e) {
			Platform.runLater(() -> {
				messageText.setText(localization.getString("unlock.errorMessage.wrongPassword"));
				passwordField.selectAll();
				passwordField.requestFocus();
			});
		} catch (UnsupportedVaultFormatException e) {
			Platform.runLater(() -> {
				if (e.isVaultOlderThanSoftware()) {
					// whitespace after localized text used as separator between text and hyperlink
					messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
					downloadsPageLink.setVisible(true);
				} else if (e.isSoftwareOlderThanVault()) {
					messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
					downloadsPageLink.setVisible(true);
				} else if (e.getDetectedVersion() == Integer.MAX_VALUE) {
					messageText.setText(localization.getString("unlock.errorMessage.unauthenticVersionMac"));
				}
			});
		} catch (FrontendCreationFailedException | CommandFailedException e) {
			LOG.error("Decryption failed for technical reasons.", e);
			Platform.runLater(() -> {
				messageText.setText(localization.getString("unlock.errorMessage.mountingFailed"));
			});
		} finally {
			Platform.runLater(() -> {
				mountName.setDisable(false);
				advancedOptionsButton.setDisable(false);
				progressIndicator.setVisible(false);
			});
		}
	}

	/* callback */

	public void setListener(UnlockListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	@FunctionalInterface
	interface UnlockListener {
		void didUnlock(Vault vault);
	}

}
