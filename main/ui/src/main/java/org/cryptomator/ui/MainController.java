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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.cryptomator.ui.InitializeController.InitializationListener;
import org.cryptomator.ui.MainModule.ControllerFactory;
import org.cryptomator.ui.UnlockController.UnlockListener;
import org.cryptomator.ui.UnlockedController.LockListener;
import org.cryptomator.ui.controls.DirectoryListCell;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class MainController implements Initializable, InitializationListener, UnlockListener, LockListener {

	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

	private Stage stage;

	@FXML
	private ContextMenu directoryContextMenu;

	@FXML
	private ContextMenu addVaultContextMenu;

	@FXML
	private HBox rootPane;

	@FXML
	private ListView<Vault> directoryList;

	@FXML
	private ToggleButton addVaultButton;

	@FXML
	private Pane contentPane;

	private ResourceBundle rb;

	private final ControllerFactory controllerFactory;

	private final Settings settings;

	@Inject
	public MainController(ControllerFactory controllerFactory, Settings settings) {
		super();
		this.controllerFactory = controllerFactory;
		this.settings = settings;
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;

		final ObservableList<Vault> items = FXCollections.observableList(settings.getDirectories());
		directoryList.setItems(items);
		directoryList.setCellFactory(this::createDirecoryListCell);
		directoryList.getSelectionModel().getSelectedItems().addListener(this::selectedDirectoryDidChange);
	}

	@FXML
	private void didClickAddVault(ActionEvent event) {
		if (addVaultContextMenu.isShowing()) {
			addVaultContextMenu.hide();
		} else {
			addVaultContextMenu.show(addVaultButton, Side.RIGHT, 0.0, 0.0);
		}
	}

	@FXML
	private void willShowAddVaultContextMenu(WindowEvent event) {
		addVaultButton.setSelected(true);
	}

	@FXML
	private void didHideAddVaultContextMenu(WindowEvent event) {
		addVaultButton.setSelected(false);
	}

	@FXML
	private void didClickCreateNewVault(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator vault", "*.cryptomator"));
		final File file = fileChooser.showSaveDialog(stage);
		try {
			if (file != null) {
				final Path vaultDir = Files.createDirectory(file.toPath());
				final Path vaultShortcutFile = vaultDir.resolve(vaultDir.getFileName());
				Files.createFile(vaultShortcutFile);
				addVault(vaultDir, true);
			}
		} catch (IOException e) {
			LOG.error("Unable to create vault", e);
		}
	}

	@FXML
	private void didClickAddExistingVaults(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator vault", "*.cryptomator"));
		final List<File> files = fileChooser.showOpenMultipleDialog(stage);
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
	void addVault(final Path path, boolean select) {
		if (path == null || !Files.isWritable(path)) {
			return;
		}

		final Path vaultPath;
		if (path != null && Files.isDirectory(path)) {
			vaultPath = path;
		} else if (Files.isRegularFile(path) && path.getParent().getFileName().toString().endsWith(Vault.VAULT_FILE_EXTENSION)) {
			vaultPath = path.getParent();
		} else {
			return;
		}

		final Vault vault = new Vault(vaultPath);
		if (!directoryList.getItems().contains(vault)) {
			directoryList.getItems().add(vault);
		}
		directoryList.getSelectionModel().select(vault);
	}

	private ListCell<Vault> createDirecoryListCell(ListView<Vault> param) {
		final DirectoryListCell cell = new DirectoryListCell();
		cell.setContextMenu(directoryContextMenu);
		return cell;
	}

	private void selectedDirectoryDidChange(ListChangeListener.Change<? extends Vault> change) {
		final Vault selectedDir = directoryList.getSelectionModel().getSelectedItem();
		if (selectedDir == null) {
			stage.setTitle(rb.getString("app.name"));
			showWelcomeView();
		} else {
			stage.setTitle(selectedDir.getName());
			showDirectory(selectedDir);
		}
	}

	@FXML
	private void didClickRemoveSelectedEntry(ActionEvent e) {
		final Vault selectedDir = directoryList.getSelectionModel().getSelectedItem();
		directoryList.getItems().remove(selectedDir);
		directoryList.getSelectionModel().clearSelection();
	}

	// ****************************************
	// Subcontroller for right panel
	// ****************************************

	private void showDirectory(Vault directory) {
		try {
			if (directory.isUnlocked()) {
				this.showUnlockedView(directory);
			} else if (directory.containsMasterKey()) {
				this.showUnlockView(directory);
			} else {
				this.showInitializeView(directory);
			}
		} catch (IOException e) {
			LOG.error("Failed to analyze directory.", e);
		}
	}

	private <T> T showView(String fxml) {
		try {
			final FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml), rb);
			loader.setControllerFactory(controllerFactory);
			final Parent root = loader.load();
			contentPane.getChildren().clear();
			contentPane.getChildren().add(root);
			return loader.getController();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load fxml file.", e);
		}
	}

	private void showWelcomeView() {
		this.showView("/fxml/welcome.fxml");
	}

	private void showInitializeView(Vault directory) {
		final InitializeController ctrl = showView("/fxml/initialize.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didInitialize(InitializeController ctrl) {
		showUnlockView(ctrl.getDirectory());
	}

	private void showUnlockView(Vault directory) {
		final UnlockController ctrl = showView("/fxml/unlock.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didUnlock(UnlockController ctrl) {
		showUnlockedView(ctrl.getDirectory());
		Platform.setImplicitExit(false);
	}

	private void showUnlockedView(Vault directory) {
		final UnlockedController ctrl = showView("/fxml/unlocked.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didLock(UnlockedController ctrl) {
		showUnlockView(ctrl.getDirectory());
		if (getUnlockedDirectories().isEmpty()) {
			Platform.setImplicitExit(true);
		}
	}

	/* Convenience */

	public Collection<Vault> getDirectories() {
		return directoryList.getItems();
	}

	public Collection<Vault> getUnlockedDirectories() {
		return getDirectories().stream().filter(d -> d.isUnlocked()).collect(Collectors.toSet());
	}

	/* public Getter/Setter */

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	/**
	 * Attempts to make the application window visible.
	 */
	public void toFront() {
		stage.setIconified(false);
		stage.show();
		stage.toFront();
	}

}
