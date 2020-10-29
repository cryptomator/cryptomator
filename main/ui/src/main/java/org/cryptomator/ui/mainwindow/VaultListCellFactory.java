package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.controls.DraggableListCell;

import javax.inject.Inject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import java.io.IOException;
import java.io.UncheckedIOException;

@MainWindowScoped
public class VaultListCellFactory implements Callback<ListView<Vault>, ListCell<Vault>> {

	private final FXMLLoaderFactory fxmlLoaders;

	@Inject
	VaultListCellFactory(@MainWindow FXMLLoaderFactory fxmlLoaders) {
		this.fxmlLoaders = fxmlLoaders;
	}

	@Override
	public ListCell<Vault> call(ListView<Vault> param) {
		try {
			FXMLLoader fxmlLoader = fxmlLoaders.load("/fxml/vault_list_cell.fxml");
			return new Cell(fxmlLoader.getRoot(), fxmlLoader.getController());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load /fxml/vault_list_cell.fxml.", e);
		}
	}

	private static class Cell extends DraggableListCell<Vault> {

		private final Parent node;
		private final VaultListCellController controller;

		public Cell(Parent node, VaultListCellController controller) {
			this.node = node;
			this.controller = controller;
		}

		@Override
		protected void updateItem(Vault item, boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setGraphic(node);
				controller.setVault(item);
			}
		}
	}
}
