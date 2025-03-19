package org.cryptomator.ui.eventview;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.event.FileSystemEventBucket;
import org.cryptomator.event.FileSystemEventRegistry;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.util.StringConverter;
import java.util.Comparator;
import java.util.ResourceBundle;

@EventViewScoped
public class EventViewController implements FxController {

	private final FileSystemEventRegistry fileSystemEventRegistry;
	private final ObservableList<FileSystemEventBucket> eventList;
	private final FilteredList<FileSystemEventBucket> filteredEventList;
	private final ObservableList<Vault> vaults;
	private final SortedList<FileSystemEventBucket> reversedEventList;
	private final ObservableList<Vault> choiceBoxEntries;
	private final ResourceBundle resourceBundle;
	private final EventListCellFactory cellFactory;

	@FXML
	ChoiceBox<Vault> vaultFilterChoiceBox;
	@FXML
	ListView<FileSystemEventBucket> eventListView;

	@Inject
	public EventViewController(FileSystemEventRegistry fileSystemEventRegistry, ObservableList<Vault> vaults, ResourceBundle resourceBundle, EventListCellFactory cellFactory) {
		this.fileSystemEventRegistry = fileSystemEventRegistry;
		this.eventList = FXCollections.observableArrayList();
		this.filteredEventList = eventList.filtered(_ -> true);
		this.vaults = vaults;
		this.reversedEventList = new SortedList<>(filteredEventList, Comparator.reverseOrder());
		this.choiceBoxEntries = FXCollections.observableArrayList();
		this.resourceBundle = resourceBundle;
		this.cellFactory = cellFactory;
	}

	@FXML
	public void initialize() {
		choiceBoxEntries.add(null);
		choiceBoxEntries.addAll(vaults);
		vaults.addListener((ListChangeListener<? super Vault>) c -> {
			while (c.next()) {
				choiceBoxEntries.removeAll(c.getRemoved());
				choiceBoxEntries.addAll(c.getAddedSubList());
			}
		});

		eventList.addAll(fileSystemEventRegistry.listAll());
		fileSystemEventRegistry.addListener((MapChangeListener<? super FileSystemEventRegistry.Key, ? super FileSystemEventRegistry.Value>) this::updateList);
		eventListView.setCellFactory(cellFactory);
		eventListView.setItems(reversedEventList);

		vaultFilterChoiceBox.setItems(choiceBoxEntries);
		vaultFilterChoiceBox.valueProperty().addListener(this::applyVaultFilter);
		vaultFilterChoiceBox.setConverter(new VaultConverter(resourceBundle));
	}

	private void updateList(MapChangeListener.Change<? extends FileSystemEventRegistry.Key, ? extends FileSystemEventRegistry.Value> change) {
		var vault = change.getKey().vault();
		if (change.wasRemoved()) {
			eventList.remove(new FileSystemEventBucket(vault, change.getValueRemoved().mostRecentEvent(), 0));
		}
		if (change.wasAdded()) {
			eventList.addLast(new FileSystemEventBucket(vault, change.getValueAdded().mostRecentEvent(), change.getValueAdded().count()));
		}
	}

	private void applyVaultFilter(ObservableValue<? extends Vault> v, Vault oldV, Vault newV) {
		if (newV == null) {
			filteredEventList.setPredicate(_ -> true);
		} else {
			filteredEventList.setPredicate(e -> e.vault().equals(newV));
		}
	}

	@FXML
	void clearEvents() {
		fileSystemEventRegistry.clear();
	}

	private static class VaultConverter extends StringConverter<Vault> {

		private final ResourceBundle resourceBundle;

		VaultConverter(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(Vault v) {
			if (v == null) {
				return resourceBundle.getString("eventView.filter.allVaults");
			} else {
				return v.getDisplayName();
			}
		}

		@Override
		public Vault fromString(String displayLanguage) {
			throw new UnsupportedOperationException();
		}
	}

}
