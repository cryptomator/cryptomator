package org.cryptomator.ui.health;

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

// unscoped because each cell needs its own controller
public class CheckListCellFactory implements Callback<ListView<Check>, ListCell<Check>> {

	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	CheckListCellFactory(@HealthCheckWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}

	@Override
	public ListCell<Check> call(ListView<Check> param) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load("/fxml/health_check_listcell.fxml");
			return new CheckListCellFactory.Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load /fxml/health_check_listcell.fxml.", e);
		}
	}

	private static class Cell extends ListCell<Check> {

		private final Parent node;
		private final CheckListCellController controller;

		public Cell(Parent node, CheckListCellController controller) {
			this.node = node;
			this.controller = controller;
		}

		@Override
		protected void updateItem(Check item, boolean empty) {
			super.updateItem(item, empty);
			if (item == null || empty) {
				setText(null);
				setGraphic(null);
			} else {
				controller.setCheck(item);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(node);
			}
		}
	}
}
