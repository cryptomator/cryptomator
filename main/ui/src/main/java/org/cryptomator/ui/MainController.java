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
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.cryptomator.ui.InitializeController.InitializationListener;
import org.cryptomator.ui.UnlockController.UnlockListener;
import org.cryptomator.ui.UnlockedController.LockListener;
import org.cryptomator.ui.controls.DirectoryListCell;
import org.cryptomator.ui.model.Directory;
import org.cryptomator.ui.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController implements Initializable, InitializationListener, UnlockListener, LockListener {

	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

	private Stage stage;

	@FXML
	private ContextMenu directoryContextMenu;

	@FXML
	private HBox rootPane;

	@FXML
	private ListView<Directory> directoryList;

	@FXML
	private Pane contentPane;

	private ResourceBundle rb;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;

		final ObservableList<Directory> items = FXCollections.observableList(Settings.load().getDirectories());
		directoryList.setItems(items);
		directoryList.setCellFactory(this::createDirecoryListCell);
		directoryList.getSelectionModel().getSelectedItems().addListener(this::selectedDirectoryDidChange);
	}

	@FXML
	private void didClickAddDirectory(ActionEvent event) {
		final DirectoryChooser dirChooser = new DirectoryChooser();
		final File file = dirChooser.showDialog(stage);
		addDirectory(file.toPath());
	}

	/**
	 * adds the given directory or selects it if it is already in the list of
	 * directories.
	 * 
	 * @param file
	 *            non-null, writable, existing directory
	 */
	void addDirectory(final Path file) {
		if (file != null && Files.isWritable(file)) {
			final Directory dir = new Directory(file);
			if (!directoryList.getItems().contains(dir)) {
				directoryList.getItems().add(dir);
			}
			directoryList.getSelectionModel().select(dir);
		}
	}

	private ListCell<Directory> createDirecoryListCell(ListView<Directory> param) {
		final DirectoryListCell cell = new DirectoryListCell();
		cell.setContextMenu(directoryContextMenu);
		return cell;
	}

	private void selectedDirectoryDidChange(ListChangeListener.Change<? extends Directory> change) {
		final Directory selectedDir = directoryList.getSelectionModel().getSelectedItem();
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
		final Directory selectedDir = directoryList.getSelectionModel().getSelectedItem();
		directoryList.getItems().remove(selectedDir);
		directoryList.getSelectionModel().clearSelection();
	}

	// ****************************************
	// Subcontroller for right panel
	// ****************************************

	private void showDirectory(Directory directory) {
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

	private void showInitializeView(Directory directory) {
		final InitializeController ctrl = showView("/fxml/initialize.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didInitialize(InitializeController ctrl) {
		showUnlockView(ctrl.getDirectory());
	}

	private void showUnlockView(Directory directory) {
		final UnlockController ctrl = showView("/fxml/unlock.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didUnlock(UnlockController ctrl) {
		showUnlockedView(ctrl.getDirectory());
		Platform.setImplicitExit(false);
	}

	private void showUnlockedView(Directory directory) {
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

	public Collection<Directory> getDirectories() {
		return directoryList.getItems();
	}

	public Collection<Directory> getUnlockedDirectories() {
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
