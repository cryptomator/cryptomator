/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.ui.controls.DirectoryListCell;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultFactory;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dagger.Lazy;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

@Singleton
public class MainController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

	private final Stage mainWindow;
	private final VaultFactory vaultFactoy;
	private final Lazy<WelcomeController> welcomeController;
	private final Lazy<InitializeController> initializeController;
	private final Lazy<NotFoundController> notFoundController;
	private final Lazy<UpgradeController> upgradeController;
	private final Lazy<UnlockController> unlockController;
	private final Provider<UnlockedController> unlockedControllerProvider;
	private final Lazy<ChangePasswordController> changePasswordController;
	private final Lazy<SettingsController> settingsController;
	private final ObjectProperty<AbstractFXMLViewController> activeController = new SimpleObjectProperty<>();
	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault = new SimpleObjectProperty<>();
	private final MonadicBinding<Boolean> isSelectedVaultUnlocked = EasyBind.select(selectedVault).selectObject(Vault::unlockedProperty);;
	private final Binding<Boolean> canEditSelectedVault = EasyBind.combine(selectedVault.isNull(), isSelectedVaultUnlocked.orElse(false), Boolean::logicalOr);
	private final BooleanBinding isShowingSettings;
	private final Map<Vault, UnlockedController> unlockedVaults = new HashMap<>();

	@Inject
	public MainController(@Named("mainWindow") Stage mainWindow, Localization localization, Settings settings, VaultFactory vaultFactoy, Lazy<WelcomeController> welcomeController,
			Lazy<InitializeController> initializeController, Lazy<NotFoundController> notFoundController, Lazy<UpgradeController> upgradeController, Lazy<UnlockController> unlockController,
			Provider<UnlockedController> unlockedControllerProvider, Lazy<ChangePasswordController> changePasswordController, Lazy<SettingsController> settingsController) {
		super(localization);
		this.mainWindow = mainWindow;
		this.vaultFactoy = vaultFactoy;
		this.welcomeController = welcomeController;
		this.initializeController = initializeController;
		this.notFoundController = notFoundController;
		this.upgradeController = upgradeController;
		this.unlockController = unlockController;
		this.unlockedControllerProvider = unlockedControllerProvider;
		this.changePasswordController = changePasswordController;
		this.settingsController = settingsController;
		this.vaults = FXCollections.observableList(settings.getDirectories());

		// derived bindings:
		this.isShowingSettings = activeController.isEqualTo(settingsController.get());
	}

	@FXML
	private ContextMenu vaultListCellContextMenu;

	@FXML
	private ContextMenu addVaultContextMenu;

	@FXML
	private HBox rootPane;

	@FXML
	private ListView<Vault> vaultList;

	@FXML
	private ToggleButton addVaultButton;

	@FXML
	private Button removeVaultButton;

	@FXML
	private ToggleButton settingsButton;

	@FXML
	private Pane contentPane;

	@FXML
	private Pane emptyListInstructions;

	@Override
	public void initialize() {
		vaultList.setItems(vaults);
		vaultList.setCellFactory(this::createDirecoryListCell);
		activeController.set(welcomeController.get());
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
		removeVaultButton.disableProperty().bind(canEditSelectedVault);
		emptyListInstructions.visibleProperty().bind(Bindings.isEmpty(vaults));

		EasyBind.subscribe(selectedVault, this::selectedVaultDidChange);
		EasyBind.subscribe(activeController, this::activeControllerDidChange);
		EasyBind.subscribe(isShowingSettings, settingsButton::setSelected);
		EasyBind.subscribe(addVaultContextMenu.showingProperty(), addVaultButton::setSelected);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/main.fxml");
	}

	private ListCell<Vault> createDirecoryListCell(ListView<Vault> param) {
		final DirectoryListCell cell = new DirectoryListCell();
		cell.setVaultContextMenu(vaultListCellContextMenu);
		return cell;
	}

	// ****************************************
	// UI Events
	// ****************************************

	@FXML
	private void didClickAddVault(ActionEvent event) {
		if (addVaultContextMenu.isShowing()) {
			addVaultContextMenu.hide();
		} else {
			addVaultContextMenu.show(addVaultButton, Side.BOTTOM, 0.0, 0.0);
		}
	}

	@FXML
	private void didClickCreateNewVault(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		final File file = fileChooser.showSaveDialog(mainWindow);
		if (file == null) {
			return;
		}
		try {
			final Path vaultDir = file.toPath();
			if (!Files.exists(vaultDir)) {
				Files.createDirectory(vaultDir);
			}
			addVault(vaultDir, true);
		} catch (IOException e) {
			LOG.error("Unable to create vault", e);
		}
	}

	@FXML
	private void didClickAddExistingVaults(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator vault", "*" + Vault.VAULT_FILE_EXTENSION));
		final List<File> files = fileChooser.showOpenMultipleDialog(mainWindow);
		if (files != null) {
			for (final File file : files) {
				addVault(file.toPath(), false);
			}
		}
	}

	/**
	 * adds the given directory or selects it if it is already in the list of directories.
	 * 
	 * @param path non-null, writable, existing directory
	 */
	public void addVault(final Path path, boolean select) {
		// TODO: `|| !Files.isWritable(path)` is broken on windows. Fix in Java 8u72, see https://bugs.openjdk.java.net/browse/JDK-8034057
		if (path == null) {
			return;
		}

		final Path vaultPath;
		if (path != null && Files.isDirectory(path)) {
			vaultPath = path;
		} else if (path != null && Files.isRegularFile(path)) {
			vaultPath = path.getParent();
		} else {
			return;
		}

		final Vault vault = vaultFactoy.createVault(vaultPath);
		if (!vaults.contains(vault)) {
			vaults.add(vault);
		}
		vaultList.getSelectionModel().select(vault);
	}

	@FXML
	private void didClickRemoveSelectedEntry(ActionEvent e) {
		vaults.remove(selectedVault.get());
		if (vaults.isEmpty()) {
			activeController.set(welcomeController.get());
		}
	}

	@FXML
	private void didClickChangePassword(ActionEvent e) {
		showChangePasswordView();
	}

	@FXML
	private void didClickShowSettings(ActionEvent e) {
		if (settingsController.get().equals(activeController.get())) {
			activeController.set(welcomeController.get());
		} else {
			activeController.set(settingsController.get());
		}
		vaultList.getSelectionModel().clearSelection();
	}

	// ****************************************
	// Binding Listeners
	// ****************************************

	private void activeControllerDidChange(AbstractFXMLViewController newValue) {
		final Parent root = newValue.loadFxml();
		contentPane.getChildren().clear();
		contentPane.getChildren().add(root);
	}

	private void selectedVaultDidChange(Vault newValue) {
		if (newValue == null) {
			return;
		}
		if (newValue.isUnlocked()) {
			this.showUnlockedView(newValue);
		} else if (!newValue.doesVaultDirectoryExist()) {
			this.showNotFoundView();
		} else if (newValue.isValidVaultDirectory() && newValue.needsUpgrade()) {
			this.showUpgradeView();
		} else if (newValue.isValidVaultDirectory()) {
			this.showUnlockView();
		} else {
			this.showInitializeView();
		}
	}

	// ****************************************
	// Public Bindings
	// ****************************************

	public Binding<String> windowTitle() {
		return EasyBind.monadic(selectedVault).flatMap(Vault::name).orElse(localization.getString("app.name"));
	}

	// ****************************************
	// Subcontroller for right panel
	// ****************************************

	private void showNotFoundView() {
		final NotFoundController ctrl = notFoundController.get();
		activeController.set(ctrl);
	}

	private void showInitializeView() {
		final InitializeController ctrl = initializeController.get();
		ctrl.vault.bind(selectedVault);
		ctrl.setListener(this::didInitialize);
		activeController.set(ctrl);
	}

	public void didInitialize() {
		showUnlockView();
	}

	private void showUpgradeView() {
		final UpgradeController ctrl = upgradeController.get();
		ctrl.vault.bind(selectedVault);
		ctrl.setListener(this::didUpgrade);
		activeController.set(ctrl);
	}

	public void didUpgrade() {
		showUnlockView();
	}

	private void showUnlockView() {
		final UnlockController ctrl = unlockController.get();
		ctrl.vault.bind(selectedVault);
		ctrl.setListener(this::didUnlock);
		activeController.set(ctrl);
	}

	public void didUnlock(Vault vault) {
		Platform.setImplicitExit(false);
		if (vault.equals(selectedVault.getValue())) {
			this.showUnlockedView(vault);
		}
	}

	private void showUnlockedView(Vault vault) {
		final UnlockedController ctrl = unlockedVaults.computeIfAbsent(vault, k -> {
			return unlockedControllerProvider.get();
		});
		ctrl.setVault(vault);
		ctrl.setListener(this::didLock);
		activeController.set(ctrl);
	}

	public void didLock(UnlockedController ctrl) {
		unlockedVaults.remove(ctrl.getVault());
		showUnlockView();
		if (!vaults.stream().anyMatch(Vault::isUnlocked)) {
			Platform.setImplicitExit(true);
		}
	}

	private void showChangePasswordView() {
		final ChangePasswordController ctrl = changePasswordController.get();
		ctrl.vault.bind(selectedVault);
		ctrl.setListener(this::didChangePassword);
		activeController.set(ctrl);
	}

	public void didChangePassword() {
		showUnlockView();
	}

}
