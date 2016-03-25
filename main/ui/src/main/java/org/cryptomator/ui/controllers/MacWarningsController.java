/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class MacWarningsController extends LocalizedFXMLViewController {

	private final Application application;
	private final ObservableList<Warning> warnings = FXCollections.observableArrayList();
	private final ListChangeListener<String> unauthenticatedResourcesChangeListener = this::unauthenticatedResourcesDidChange;
	private final ChangeListener<Boolean> stageVisibilityChangeListener = this::windowVisibilityDidChange;
	final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private Stage stage;

	@Inject
	public MacWarningsController(Application application, Localization localization) {
		super(localization);
		this.application = application;
	}

	@FXML
	private ListView<Warning> warningsList;

	@FXML
	private Button whitelistButton;

	@Override
	public void initialize() {
		warnings.addListener(this::warningsDidInvalidate);
		warningsList.setItems(warnings);
		warningsList.setCellFactory(CheckBoxListCell.forListView(Warning::selectedProperty, new StringConverter<Warning>() {

			@Override
			public String toString(Warning object) {
				return object.getName();
			}

			@Override
			public Warning fromString(String string) {
				return null;
			}

		}));
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/mac_warnings.fxml");
	}

	@Override
	public void initStage(Stage stage) {
		super.initStage(stage);
		this.stage = stage;
		stage.showingProperty().addListener(new WeakChangeListener<>(stageVisibilityChangeListener));
	}

	@FXML
	private void didClickWhitelistButton(ActionEvent event) {
		warnings.filtered(w -> w.isSelected()).stream().forEach(w -> {
			final String resourceToBeWhitelisted = w.getName();
			vault.get().getWhitelistedResourcesWithInvalidMac().add(resourceToBeWhitelisted);
			vault.get().getNamesOfResourcesWithInvalidMac().remove(resourceToBeWhitelisted);
		});
		warnings.removeIf(w -> w.isSelected());
	}

	@FXML
	private void didClickMoreInformationButton(ActionEvent event) {
		application.getHostServices().showDocument("https://cryptomator.org/faq/#macWarning");
	}

	private void unauthenticatedResourcesDidChange(Change<? extends String> change) {
		while (change.next()) {
			if (change.wasAdded()) {
				warnings.addAll(change.getAddedSubList().stream().map(Warning::new).collect(Collectors.toList()));
			} else if (change.wasRemoved()) {
				change.getRemoved().forEach(str -> {
					warnings.removeIf(w -> str.equals(w.name.get()));
				});
			}
		}
	}

	private void warningsDidInvalidate(Observable observable) {
		disableWhitelistButtonIfNothingSelected();
	}

	private void windowVisibilityDidChange(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		if (Boolean.TRUE.equals(newValue)) {
			stage.setTitle(String.format(localization.getString("macWarnings.windowTitle"), vault.get().name().getValue()));
			warnings.addAll(vault.get().getNamesOfResourcesWithInvalidMac().stream().map(Warning::new).collect(Collectors.toList()));
			vault.get().getNamesOfResourcesWithInvalidMac().addListener(this.unauthenticatedResourcesChangeListener);
		} else {
			vault.get().getNamesOfResourcesWithInvalidMac().clear();
			vault.get().getNamesOfResourcesWithInvalidMac().removeListener(this.unauthenticatedResourcesChangeListener);
		}
	}

	private void disableWhitelistButtonIfNothingSelected() {
		whitelistButton.setDisable(warnings.filtered(w -> w.isSelected()).isEmpty());
	}

	private class Warning {

		private final ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
		private final BooleanProperty selected = new SimpleBooleanProperty(false);

		public Warning(String name) {
			this.name.set(name);
			this.selectedProperty().addListener(change -> {
				disableWhitelistButtonIfNothingSelected();
			});
		}

		public String getName() {
			return name.get();
		}

		public BooleanProperty selectedProperty() {
			return selected;
		}

		public boolean isSelected() {
			return selected.get();
		}

	}

}
