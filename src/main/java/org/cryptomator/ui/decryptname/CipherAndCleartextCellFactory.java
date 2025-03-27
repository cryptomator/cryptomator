package org.cryptomator.ui.decryptname;

import jakarta.inject.Inject;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import java.io.IOException;
import java.io.UncheckedIOException;

@DecryptNameScoped
public class CipherAndCleartextCellFactory implements Callback<ListView<CipherAndCleartext>, ListCell<CipherAndCleartext>> {

	private static final String FXML_PATH = "/fxml/decryptnames_cipherandcleartextcell.fxml";
	private final FxmlLoaderFactory fxmlLoaders;

	@Inject
	public CipherAndCleartextCellFactory(@DecryptNameWindow FxmlLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}


	@Override
	public ListCell<CipherAndCleartext> call(ListView<CipherAndCleartext> cipherAndCleartextListView) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load(FXML_PATH);
			return new Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load %s.".formatted(FXML_PATH), e);
		}
	}

	private static class Cell extends ListCell<CipherAndCleartext> {

		private final Parent root;
		private final CipherAndCleartextCellController controller;

		public Cell(Parent root, CipherAndCleartextCellController controller) {
			this.root = root;
			this.controller = controller;
		}

		@Override
		protected void updateItem(CipherAndCleartext item, boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null) {
				setGraphic(null);
				this.getStyleClass().remove("test-list-cell");
				setVisible(false);
			} else {
				this.getStyleClass().addLast("test-list-cell");
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(root);
				controller.setCipherAndCleartextEntry(item);
				setVisible(true);
			}
		}
	}
}