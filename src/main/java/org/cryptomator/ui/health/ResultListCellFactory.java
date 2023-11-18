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

@HealthCheckScoped
public class ResultListCellFactory implements Callback<ListView<Result>, ListCell<Result>> {

	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	ResultListCellFactory(@HealthCheckWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}

	// using push down variable technique to remove the unnecessary abstraction code smell
	// removed the nested class as they are unnecessary and not required
	@Override
	public ListCell<Result> call(ListView<Result> param) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load("/fxml/health_result_listcell.fxml");
			Parent node = fxmlLoader.getRoot();
			ResultListCellController controller = fxmlLoader.getController();

			return new ListCell<Result>() {
				@Override
				protected void updateItem(Result item, boolean empty) {
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
			};
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load /fxml/health_result_listcell.fxml.", e);
		}
	}
}