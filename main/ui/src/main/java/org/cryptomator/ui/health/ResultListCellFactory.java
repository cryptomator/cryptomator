package org.cryptomator.ui.health;


import org.cryptomator.cryptofs.health.api.DiagnosticResult;
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

@HealthCheckScoped
public class ResultListCellFactory implements Callback<ListView<DiagnosticResult>, ListCell<DiagnosticResult>> {

	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	ResultListCellFactory(@HealthCheckWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}

	@Override
	public ListCell<DiagnosticResult> call(ListView<DiagnosticResult> param) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load("/fxml/health_result_listcell.fxml");
			return new ResultListCellFactory.Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load /fxml/health_result_listcell.fxml.", e);
		}
	}

	private static class Cell extends ListCell<DiagnosticResult> {

		private final Parent node;
		private final ResultListCellController controller;

		public Cell(Parent node, ResultListCellController controller) {
			this.node = node;
			this.controller = controller;
		}

		@Override
		protected void updateItem(DiagnosticResult item, boolean empty) {
			super.updateItem(item, empty);
			if (item == null || empty) {
				setText(null);
				setGraphic(null);
			} else {
				controller.setResult(item);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(node);
			}
		}
	}
}