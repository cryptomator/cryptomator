package org.cryptomator.ui.eventview;

import org.cryptomator.event.FileSystemEventBucket;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javax.inject.Inject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import java.io.IOException;
import java.io.UncheckedIOException;

@EventViewScoped
public class EventListCellFactory implements Callback<ListView<FileSystemEventBucket>, ListCell<FileSystemEventBucket>> {

	private static final String FXML_PATH = "/fxml/eventview_cell.fxml";

	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	EventListCellFactory(@EventViewWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}


	@Override
	public ListCell<FileSystemEventBucket> call(ListView<FileSystemEventBucket> eventListView) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load(FXML_PATH);
			return new Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load %s.".formatted(FXML_PATH), e);
		}
	}

	private static class Cell extends ListCell<FileSystemEventBucket> {

		private final Parent root;
		private final EventListCellController controller;

		public Cell(Parent root, EventListCellController controller) {
			this.root = root;
			this.controller = controller;
		}

		@Override
		protected void updateItem(FileSystemEventBucket item, boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null) {
				setGraphic(null);
				this.getStyleClass().remove("list-cell");
			} else {
				this.getStyleClass().addLast("list-cell");
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(root);
				controller.setEvent(item);
			}
		}
	}
}
