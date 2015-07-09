package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.inject.Inject;

import org.cryptomator.ui.model.Vault;

public class MacWarningsController implements Initializable {

	@FXML
	private ListView<Warning> warningsList;

	@FXML
	private Button whitelistButton;

	private final Application application;
	private final ObservableList<Warning> warnings = FXCollections.observableArrayList();
	private final ListChangeListener<String> unauthenticatedResourcesChangeListener = this::unauthenticatedResourcesDidChange;
	private final ChangeListener<Boolean> stageVisibilityChangeListener = this::windowVisibilityDidChange;
	private Stage stage;
	private Vault vault;
	private ResourceBundle rb;

	@Inject
	public MacWarningsController(Application application) {
		this.application = application;
	}

	@Override
	public void initialize(URL location, ResourceBundle rb) {
		this.rb = rb;
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

	@FXML
	private void didClickWhitelistButton(ActionEvent event) {
		warnings.filtered(w -> w.isSelected()).stream().forEach(w -> {
			final String resourceToBeWhitelisted = w.getName();
			vault.getWhitelistedResourcesWithInvalidMac().add(resourceToBeWhitelisted);
			vault.getNamesOfResourcesWithInvalidMac().remove(resourceToBeWhitelisted);
		});
		warnings.removeIf(w -> w.isSelected());
	}

	@FXML
	private void didClickMoreInformationButton(ActionEvent event) {
		application.getHostServices().showDocument("https://cryptomator.org/help.html#macWarning");
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
			stage.setTitle(String.format(rb.getString("macWarnings.windowTitle"), vault.getName()));
			warnings.addAll(vault.getNamesOfResourcesWithInvalidMac().stream().map(Warning::new).collect(Collectors.toList()));
			vault.getNamesOfResourcesWithInvalidMac().addListener(this.unauthenticatedResourcesChangeListener);
		} else {
			vault.getNamesOfResourcesWithInvalidMac().clear();
			vault.getNamesOfResourcesWithInvalidMac().removeListener(this.unauthenticatedResourcesChangeListener);
		}
	}

	private void disableWhitelistButtonIfNothingSelected() {
		whitelistButton.setDisable(warnings.filtered(w -> w.isSelected()).isEmpty());
	}

	public void setStage(Stage stage) {
		this.stage = stage;
		stage.showingProperty().addListener(new WeakChangeListener<>(stageVisibilityChangeListener));
	}

	public void setVault(Vault vault) {
		this.vault = vault;
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
