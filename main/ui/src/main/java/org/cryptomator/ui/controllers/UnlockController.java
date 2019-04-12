/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.keychain.KeychainAccess;
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

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class UnlockController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);
	private static final CharMatcher ALPHA_NUMERIC_MATCHER = CharMatcher.inRange('a', 'z') //
			.or(CharMatcher.inRange('A', 'Z')) //
			.or(CharMatcher.inRange('0', '9')) //
			.or(CharMatcher.is('_')) //
			.precomputed();

	private final Application app;
	private final Stage mainWindow;
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
	public UnlockController(Application app, @Named("mainWindow") Stage mainWindow, Localization localization, WindowsDriveLetters driveLetters, Optional<KeychainAccess> keychainAccess, Settings settings, ExecutorService executor) {
		this.app = app;
		this.mainWindow = mainWindow;
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
	private Text messageText;

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
	private CheckBox useCustomMountPoint;

	@FXML
	private HBox customMountPoint;

	@FXML
	private Label customMountPointLabel;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Text progressText;

	@FXML
	private Hyperlink downloadsPageLink;

	@FXML
	private GridPane advancedOptions;

	@FXML
	private GridPane root;

	@FXML
	private CheckBox unlockAfterStartup;

	@FXML
	private CheckBox useReadOnlyMode;

	@Override
	public void initialize() {
		advancedOptions.managedProperty().bind(advancedOptions.visibleProperty());
		unlockButton.disableProperty().bind(passwordField.textProperty().isEmpty());
		mountName.addEventFilter(KeyEvent.KEY_TYPED, this::filterAlphanumericKeyEvents);
		mountName.textProperty().addListener(this::mountNameDidChange);
		savePassword.setDisable(!keychainAccess.isPresent());
		unlockAfterStartup.disableProperty().bind(savePassword.disabledProperty().or(savePassword.selectedProperty().not()));

		customMountPoint.visibleProperty().bind(useCustomMountPoint.selectedProperty());
		customMountPoint.managedProperty().bind(useCustomMountPoint.selectedProperty());
		winDriveLetter.setConverter(new WinDriveLetterLabelConverter());

		if (!SystemUtils.IS_OS_WINDOWS) {
			winDriveLetterLabel.setVisible(false);
			winDriveLetterLabel.setManaged(false);
			winDriveLetter.setVisible(false);
			winDriveLetter.setManaged(false);
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
		progressText.setText(null);
		state.successMessage().map(localization::getString).ifPresent(messageText::setText);
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.valueProperty().removeListener(driveLetterChangeListener);
			winDriveLetter.getItems().clear();
			winDriveLetter.getItems().add(null);
			winDriveLetter.getItems().addAll(driveLetters.getAvailableDriveLetters());
			winDriveLetter.getItems().sort(new WinDriveLetterComparator());
			winDriveLetter.valueProperty().addListener(driveLetterChangeListener);
			chooseSelectedDriveLetter();
		}
		downloadsPageLink.setVisible(false);
		mountName.setText(vault.getMountName());
		savePassword.setSelected(false);
		// auto-fill pw from keychain:
		if (keychainAccess.isPresent()) {
			char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId());
			if (storedPw != null) {
				savePassword.setSelected(true);
				passwordField.setPassword(storedPw);
				passwordField.selectRange(storedPw.length, storedPw.length);
				Arrays.fill(storedPw, ' ');
			}
		}
		VaultSettings vaultSettings = vault.getVaultSettings();
		unlockAfterStartup.setSelected(savePassword.isSelected() && vaultSettings.unlockAfterStartup().get());
		revealAfterMount.setSelected(vaultSettings.revealAfterMount().get());

		// WEBDAV-dependent controls:
		if (VolumeImpl.WEBDAV.equals(settings.preferredVolumeImpl().get())) {
			useCustomMountPoint.setVisible(false);
			useCustomMountPoint.setManaged(false);
		} else {
			useCustomMountPoint.setVisible(true);
			useCustomMountPoint.setSelected(vaultSettings.usesIndividualMountPath().get());
			if (Strings.isNullOrEmpty(vaultSettings.individualMountPath().get())) {
				customMountPointLabel.setText(localization.getString("unlock.label.chooseMountPath"));
			} else {
				customMountPointLabel.setText(displayablePath(vaultSettings.individualMountPath().getValueSafe()));
			}
		}

		// DOKANY-dependent controls:
		if (VolumeImpl.DOKANY.equals(settings.preferredVolumeImpl().get())) {
			winDriveLetter.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetter.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
			// readonly not yet supported by dokany
			useReadOnlyMode.setSelected(false);
			useReadOnlyMode.setVisible(false);
			useReadOnlyMode.setManaged(false);
		} else {
			useReadOnlyMode.setSelected(vaultSettings.usesReadOnlyMode().get());
		}

		// OS-dependent controls:
		if (SystemUtils.IS_OS_WINDOWS) {
			winDriveLetter.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetter.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.visibleProperty().bind(useCustomMountPoint.selectedProperty().not());
			winDriveLetterLabel.managedProperty().bind(useCustomMountPoint.selectedProperty().not());
		}

		vaultSubs = vaultSubs.and(EasyBind.subscribe(unlockAfterStartup.selectedProperty(), vaultSettings.unlockAfterStartup()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(revealAfterMount.selectedProperty(), vaultSettings.revealAfterMount()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(useCustomMountPoint.selectedProperty(), vaultSettings.usesIndividualMountPath()::set));
		vaultSubs = vaultSubs.and(EasyBind.subscribe(useReadOnlyMode.selectedProperty(), vaultSettings.usesReadOnlyMode()::set));
	}

	private String displayablePath(String path) {
		Path homeDir = Paths.get(SystemUtils.USER_HOME);
		Path p = Paths.get(path);
		if (p.startsWith(homeDir)) {
			Path relativePath = homeDir.relativize(p);
			String homePrefix = SystemUtils.IS_OS_WINDOWS ? "~\\" : "~/";
			return homePrefix + relativePath.toString();
		} else {
			return p.toString();
		}
	}

	// ****************************************
	// Downloads link
	// ****************************************

	@FXML
	public void didClickDownloadsLink() {
		app.getHostServices().showDocument("https://cryptomator.org/downloads/#allVersions");
	}

	// ****************************************
	// Advanced options button
	// ****************************************

	@FXML
	private void didClickAdvancedOptionsButton() {
		messageText.setText(null);
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

	private void mountNameDidChange(@SuppressWarnings("unused") ObservableValue<? extends String> property, @SuppressWarnings("unused")String oldValue, String newValue) {
		// newValue is guaranteed to be a-z0-9_, see #filterAlphanumericKeyEvents
		if (newValue.isEmpty()) {
			mountName.setText(vault.getMountName());
		} else {
			vault.setMountName(newValue);
		}
	}

	@FXML
	public void didClickChooseCustomMountPoint() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		File file = dirChooser.showDialog(mainWindow);
		if (file != null) {
			customMountPointLabel.setText(displayablePath(file.toString()));
			vault.setCustomMountPath(file.toString());
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
				return letter + ":";
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

	private void winDriveLetterDidChange(@SuppressWarnings("unused") ObservableValue<? extends Character> property, @SuppressWarnings("unused") Character oldValue, Character newValue) {
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
	private void didClickSavePasswordCheckbox() {
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
	private void didClickUnlockButton() {
		advancedOptions.setDisable(true);
		advancedOptions.setVisible(false);
		advancedOptionsButton.setText(localization.getString("unlock.button.advancedOptions.show"));
		progressIndicator.setVisible(true);

		CharSequence password = passwordField.getCharacters();
		Tasks.create(() -> {
			progressText.setText(localization.getString("unlock.pendingMessage.unlocking"));
			vault.unlock(password);
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				keychainAccess.get().storePassphrase(vault.getId(), password);
			}
		}).onSuccess(() -> {
			messageText.setText("");
			downloadsPageLink.setVisible(false);
			listener.ifPresent(lstnr -> lstnr.didUnlock(vault));
			passwordField.swipe();
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
		}).onError(NotDirectoryException.class, e -> {
			LOG.error("Unlock failed. Mount point not a directory: {}", e.getMessage());
			advancedOptions.setVisible(true);
			messageText.setText(null);
			showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNonExisting");
		}).onError(DirectoryNotEmptyException.class, e -> {
			LOG.error("Unlock failed. Mount point not empty: {}", e.getMessage());
			advancedOptions.setVisible(true);
			messageText.setText(null);
			showUnlockFailedErrorDialog("unlock.failedDialog.content.mountPathNotEmpty");
		}).onError(Exception.class, e -> { // including RuntimeExceptions
			LOG.error("Unlock failed for technical reasons.", e);
			messageText.setText(localization.getString("unlock.errorMessage.unlockFailed"));
		}).andFinally(() -> {
			advancedOptions.setDisable(false);
			progressIndicator.setVisible(false);
			progressText.setText(null);
		}).runOnce(executor);
	}

	private void showUnlockFailedErrorDialog(String localizableContentKey) {
		String title = localization.getString("unlock.failedDialog.title");
		String header = localization.getString("unlock.failedDialog.header");
		String content = localization.getString(localizableContentKey);
		Alert alert = DialogBuilderUtil.buildErrorDialog(title, header, content, ButtonType.OK);
		alert.show();
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
		UNLOCKING(null), //
		INITIALIZED("unlock.successLabel.vaultCreated"), //
		PASSWORD_CHANGED("unlock.successLabel.passwordChanged"), //
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
