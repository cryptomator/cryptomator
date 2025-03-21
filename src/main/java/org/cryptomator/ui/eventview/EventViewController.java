package org.cryptomator.ui.eventview;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.event.FileSystemEventAggregator;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.EventsUpdateCheck;

import javax.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.util.StringConverter;
import java.util.Map;
import java.util.ResourceBundle;

@EventViewScoped
public class EventViewController implements FxController {

	private final FilteredList<Map.Entry<FileSystemEventAggregator.Key, FileSystemEventAggregator.Value>> filteredEventList;
	private final ObservableList<Vault> vaults;
	private final SortedList<Map.Entry<FileSystemEventAggregator.Key, FileSystemEventAggregator.Value>> sortedEventList;
	private final ObservableList<Vault> choiceBoxEntries;
	private final ResourceBundle resourceBundle;
	private final EventListCellFactory cellFactory;

	@FXML
	ChoiceBox<Vault> vaultFilterChoiceBox;
	@FXML
	ListView<Map.Entry<FileSystemEventAggregator.Key, FileSystemEventAggregator.Value>> eventListView;

	@Inject
	public EventViewController(EventsUpdateCheck eventsUpdateCheck, ObservableList<Vault> vaults, ResourceBundle resourceBundle, EventListCellFactory cellFactory) {
		this.filteredEventList = eventsUpdateCheck.getList().filtered(_ -> true);
		this.vaults = vaults;
		this.sortedEventList = new SortedList<>(filteredEventList, this::compareBuckets);
		this.choiceBoxEntries = FXCollections.observableArrayList();
		this.resourceBundle = resourceBundle;
		this.cellFactory = cellFactory;
	}

	/**
	 * Comparsion method for the lru cache. During comparsion the map is accessed.
	 * First the entries are compared by the event timestamp, then vaultId, then identifying path and lastly by class name.
	 *
	 * @param left a {@link FileSystemEventAggregator.Key} object
	 * @param right another {@link FileSystemEventAggregator.Key} object, compared to {@code left}
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	private int compareBuckets(Map.Entry<FileSystemEventAggregator.Key, FileSystemEventAggregator.Value> left, Map.Entry<FileSystemEventAggregator.Key, FileSystemEventAggregator.Value> right) {
		var t1 = left.getValue().mostRecentEvent().getTimestamp();
		var t2 = right.getValue().mostRecentEvent().getTimestamp();
		var timeComparison = t1.compareTo(t2);
		if (timeComparison != 0) {
			return -timeComparison; //we need the reverse timesorting
		}
		var vaultIdComparsion = left.getKey().vault().getId().compareTo(right.getKey().vault().getId());
		if (vaultIdComparsion != 0) {
			return vaultIdComparsion;
		}
		var pathComparsion = left.getKey().idPath().compareTo(right.getKey().idPath());
		if (pathComparsion != 0) {
			return pathComparsion;
		}
		return left.getKey().c().getName().compareTo(right.getKey().c().getName());
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

		eventListView.setCellFactory(cellFactory);
		eventListView.setItems(sortedEventList);

		vaultFilterChoiceBox.setItems(choiceBoxEntries);
		vaultFilterChoiceBox.valueProperty().addListener(this::applyVaultFilter);
		vaultFilterChoiceBox.setConverter(new VaultConverter(resourceBundle));
	}

	private void applyVaultFilter(ObservableValue<? extends Vault> v, Vault oldV, Vault newV) {
		if (newV == null) {
			filteredEventList.setPredicate(_ -> true);
		} else {
			filteredEventList.setPredicate(e -> e.getKey().vault().equals(newV));
		}
	}

	@FXML
	void clearEvents() {
		//fileSystemEventRegistry.clear();
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
