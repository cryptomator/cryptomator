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
import java.util.ResourceBundle;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
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
	private HBox rootPane;

	@FXML
	private ListView<Directory> directoryList;

	@FXML
	private Pane contentPane;

	private ResourceBundle rb;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.rb = rb;
		directoryList.setCellFactory(this::createDirecoryListCell);
		directoryList.getSelectionModel().getSelectedItems().addListener(this::selectedDirectoryDidChange);
		directoryList.getItems().addAll(Settings.load().getDirectories());
	}

	@FXML
	private void didClickAddDirectory(ActionEvent event) {
		final DirectoryChooser dirChooser = new DirectoryChooser();
		final File file = dirChooser.showDialog(stage);
		if (file != null && file.canWrite()) {
			final Directory dir = new Directory(file.toPath());
			directoryList.getItems().add(dir);
			Settings.load().getDirectories().clear();
			Settings.load().getDirectories().addAll(directoryList.getItems());
			directoryList.getSelectionModel().selectLast();
		}
	}

	private ListCell<Directory> createDirecoryListCell(ListView<Directory> param) {
		return new DirectoryListCell();
	}

	private void selectedDirectoryDidChange(ListChangeListener.Change<? extends Directory> change) {
		final Directory selectedDir = directoryList.getSelectionModel().getSelectedItem();
		stage.setTitle(selectedDir.getName());
		try {
			if (selectedDir.containsMasterKey()) {
				this.showUnlockView(selectedDir);
			} else {
				this.showInitializeView(selectedDir);
			}
		} catch (IOException e) {
			LOG.error("Failed to analyze directory.", e);
		}
	}

	// ****************************************
	// Subcontroller for right panel
	// ****************************************

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

	private void showInitializeView(Directory directory) {
		final InitializeController ctrl = showView("/initialize.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didInitialize(InitializeController ctrl) {
		showUnlockView(ctrl.getDirectory());
	}

	private void showUnlockView(Directory directory) {
		final UnlockController ctrl = showView("/unlock.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didUnlock(UnlockController ctrl) {
		showUnlockedView(ctrl.getDirectory());
	}

	private void showUnlockedView(Directory directory) {
		final UnlockedController ctrl = showView("/unlocked.fxml");
		ctrl.setDirectory(directory);
		ctrl.setListener(this);
	}

	@Override
	public void didLock(UnlockedController ctrl) {
		showUnlockView(ctrl.getDirectory());
	}

	/* public Getter/Setter */

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

}
