package org.cryptomator.ui.eventview;

import org.cryptomator.event.FSEventBucket;
import org.cryptomator.event.FSEventBucketContent;
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
import java.util.Map;

@EventViewScoped
public class EventListCellFactory implements Callback<ListView<Map.Entry<FSEventBucket, FSEventBucketContent>>, ListCell<Map.Entry<FSEventBucket, FSEventBucketContent>>> {

	private static final String FXML_PATH = "/fxml/eventview_cell.fxml";

	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	EventListCellFactory(@EventViewWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}


	@Override
	public ListCell<Map.Entry<FSEventBucket, FSEventBucketContent>> call(ListView<Map.Entry<FSEventBucket, FSEventBucketContent>> eventListView) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load(FXML_PATH);
			return new Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load %s.".formatted(FXML_PATH), e);
		}
	}

	private static class Cell extends ListCell<Map.Entry<FSEventBucket, FSEventBucketContent>> {

		private final Parent root;
		private final EventListCellController controller;

		public Cell(Parent root, EventListCellController controller) {
			this.root = root;
			this.controller = controller;
		}

		@Override
		protected void updateItem(Map.Entry<FSEventBucket, FSEventBucketContent> item, boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null) {
				setGraphic(null);
				this.getStyleClass().remove("list-cell");
			} else {
				this.getStyleClass().addLast("list-cell");
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(root);
				controller.setEventEntry(item);
			}
		}
	}
}
