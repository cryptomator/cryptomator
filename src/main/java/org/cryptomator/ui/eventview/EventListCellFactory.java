package org.cryptomator.ui.eventview;

import org.cryptomator.event.Event;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.mainwindow.VaultListCellController;

import javax.inject.Inject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import java.io.IOException;
import java.io.UncheckedIOException;

public class EventListCellFactory implements Callback<ListView<Event>, ListCell<Event>> {

	private static final String FXML_PATH = "/fxml/eventview_cell.fxml";

	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	EventListCellFactory(@EventViewWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}


	@Override
	public ListCell<Event> call(ListView<Event> eventListView) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load(FXML_PATH);
			return new Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load %s.".formatted(FXML_PATH), e);
		}
	}

	private static class Cell extends ListCell<Event> {

		private final Parent root;
		private final EventListCellController controller;

		public Cell(Parent root, EventListCellController controller) {
			this.root = root;
			this.controller = controller;
		}

		@Override
		protected void updateItem(Event item, boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(root);
				controller.setEvent(item);
			}
		}
	}
}
