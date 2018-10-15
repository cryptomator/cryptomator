/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
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
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.frontend.webdav.ServerLifecycleException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.model.InvalidSettingsException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.WindowsDriveLetters;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.cryptomator.ui.util.Tasks;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);
	private static final CharMatcher ALPHA_NUMERIC_MATCHER = CharMatcher.inRange('a', 'z') //
			.or(CharMatcher.inRange('A', 'Z')) //
			.or(CharMatcher.inRange('0', '9')) //
			.or(CharMatcher.is('_')) //
			.precomputed();

	private final Application app;
	private final Localization localization;
	private final WindowsDriveLetters driveLetters;
	private final ChangeListener<Character> driveLetterChangeListener = this::winDriveLetterDidChange;
	private final Optional<KeychainAccess> keychainAccess;
	private final Settings settings;
	private final ExecutorService executor;
	private Vault vault;
	private Optional<UnlockListener> listener = Optional.empty();
	private Subscription vaultSubs = Subscription.EMPTY;

	@Inject
	public UnlockController(Application app, Localization localization, WindowsDriveLetters driveLetters, Optional<KeychainAccess> keychainAccess, Settings settings, ExecutorService executor) {
		this.app = app;
		this.localization = localization;
		this.driveLetters = driveLetters;
		this.keychainAccess = keychainAccess;
		this.settings = settings;
		this.executor = executor;
	}

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private Button advancedOptionsButton;

	@FXML
	private Button unlockButton;

	@FXML
	private Label successMessage;

	@FXML
	private CheckBox savePassword;

	@FXML
	private TextField mountName;

	@FXML
	private CheckBox revealAfterMount;

	@FXML
	private Label winDriveLetterLabel;

	@FXML
	private ChoiceBox<Character> winDriveLetter;

	@FXML
	private CheckBox useOwnMountPath;

	@FXML
	private Label mountPathLabel;

	@FXML
	private TextField mountPath;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Text messageText;

	@FXML
	private Hyperlink downloadsPageLink;

	@FXML
	private GridPane advancedOptions;

	@FXML
	private GridPane root;

	@FXML
	private CheckBox unlockAfterStartup;

	@Override
	public void initialize() {
		advancedOptions.managedProperty().bind(advancedOptions.visibleProperty());
		unlockButton.disableProperty().bind(passwordField.textProperty().isEmpty());
		mountName.addEventFilter(KeyEvent.KEY_TYPED, this::filterAlphanumericKeyEvents);
		mountName.textProperty().addListener(this::mountNameDidChange);
		savePassword.setDisable(!keychainAccess.isPresent());
		unlockAfterStartup.disableProperty().bind(savePassword.disabledProperty().or(savePassword.selectedProperty().not()));

		mountPathLabel.visibleProperty().bind(useOwnMountPath.selectedProperty());
		mountPath.visibleProperty().bind(useOwnMountPath.selectedProperty());
		mountPath.managedProperty().bind(useOwnMountPath.selectedProperty());
		mountPath.textProperty().addListener(this::mountPathDidChange);

		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.setConverter(new WinDriveLetterLabelConverter());
			useOwnMountPath.setVisible(false);
			useOwnMountPath.setManaged(false);
			mountPathLabel.setManaged(false);
			//dirty cheat
			mountPath.setMouseTransparent(true);
		} else {
			winDriveLetterLabel.setVisible(false);
			winDriveLetterLabel.setManaged(false);
			winDriveLetter.setVisible(false);
			winDriveLetter.setManaged(false);
			if (VolumeImpl.WEBDAV.equals(settings.preferredVolumeImpl().get())) {
				useOwnMountPath.setVisible(false);
				useOwnMountPath.setManaged(false);
				mountPathLabel.setManaged(false);
			}
		}
	}


	@Override
	public Parent getRoot() {
		return root;
	}

	@Override
	public void focus() {
		passwordField.requestFocus();
	}

	void setVault(Vault vault, State state) {
		vaultSubs.unsubscribe();
		vaultSubs = Subscription.EMPTY;

		// trigger "default" change to refresh key bindings:
		unlockButton.setDefaultButton(false);
		unlockButton.setDefaultButton(true);
		if (Objects.equals(this.vault, Objects.requireNonNull(vault))) {
			return;
		}
		assert vault != null;
		this.vault = vault;
		advancedOptions.setVisible(false);
		advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
		progressIndicator.setVisible(false);
		successMessage.setVisible(state.successMessage().isPresent());
		state.successMessage().map(localization::getString).ifPresent(successMessage::setText);
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
		VaultSettings vaultSettings = vault.getVaultSettings();
		unlockAfterStartup.setSelected(savePassword.isSelected() && vaultSettings.unlockAfterStartup().get());
		revealAfterMount.setSelected(vaultSettings.revealAfterMount().get());
		useOwnMountPath.setSelected(vaultSettings.usesIndividualMountPath().get());

		vaultSubs = vaultSubs.and(EasyBind.subscribe(unlockAfterStartup.selectedProperty(), vaultSettings.unlockAfterStartup()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(revealAfterMount.selectedProperty(), vaultSettings.revealAfterMount()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(useOwnMountPath.selectedProperty(), vaultSettings.usesIndividualMountPath()::set));


		mountPath.textProperty().setValue(vaultSettings.individualMountPath().getValueSafe());

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
		successMessage.setVisible(false);
		advancedOptions.setVisible(!advancedOptions.isVisible());
		if (advancedOptions.isVisible()) {
			advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.hide"));
		} else {
			advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
		}
	}

	private void filterAlphanumericKeyEvents(KeyEvent t) {
		if (!Strings.isNullOrEmpty(t.getCharacter()) && !ALPHA_NUMERIC_MATCHER.matchesAllOf(t.getCharacter())) {
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

	private void mountPathDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		vault.setIndividualMountPath(newValue);
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
		advancedOptions.setDisable(true);
		advancedOptions.setVisible(false);
		progressIndicator.setVisible(true);

		CharSequence password = passwordField.getCharacters();
		Tasks.create(() -> {
			vault.unlock(password);
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				keychainAccess.get().storePassphrase(vault.getId(), password);
			}
		}).onSuccess(() -> {
			messageText.setText(null);
			downloadsPageLink.setVisible(false);
			listener.ifPresent(lstnr -> lstnr.didUnlock(vault));
		}).onError(InvalidSettingsException.class, e -> {
			messageText.setText(localization.getString("unlock.errorMessage.invalidMountPath"));
			advancedOptions.setVisible(true);
			mountPath.setStyle("-fx-border-color: red;");
		}).onError(InvalidPassphraseException.class, e -> {
			messageText.setText(localization.getString("unlock.errorMessage.wrongPassword"));
			passwordField.selectAll();
			passwordField.requestFocus();
		}).onError(UnsupportedVaultFormatException.class, e -> {
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
		}).onError(ServerLifecycleException.class, e -> {
			LOG.error("Unlock failed for technical reasons.", e);
			messageText.setText(localization.getString("unlock.errorMessage.unlockFailed"));
		}).onError(Exception.class, e -> {
			LOG.error("Unlock failed for technical reasons.", e);
			messageText.setText(localization.getString("unlock.errorMessage.unlockFailed"));
		}).andFinally(() -> {
			if (!savePassword.isSelected()) {
				passwordField.swipe();
			}
			advancedOptions.setDisable(false);
			progressIndicator.setVisible(false);
			if (advancedOptions.isVisible()) { //dirty programming, but otherwise the focus is wrong
				mountPath.requestFocus();
			}
		}).runOnce(executor);
	}

	/* callback */

	public void setListener(UnlockListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	@FunctionalInterface
	interface UnlockListener {

		void didUnlock(Vault vault);
	}

	/* state */

	public enum State {
		UNLOCKING(null),
		INITIALIZED("unlock.successLabel.vaultCreated"),
		PASSWORD_CHANGED("unlock.successLabel.passwordChanged"),
		UPGRADED("unlock.successLabel.upgraded");

		private Optional<String> successMessage;

		State(String successMessage) {
			this.successMessage = Optional.ofNullable(successMessage);
		}

		public Optional<String> successMessage() {
			return successMessage;
		}

	}

}
