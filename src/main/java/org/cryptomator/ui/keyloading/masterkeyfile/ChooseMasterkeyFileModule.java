package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.keyloading.KeyLoading;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

@Module
interface ChooseMasterkeyFileModule {

	@Provides
	@ChooseMasterkeyFileScoped
	static CompletableFuture<Path> provideResult() {
		return new CompletableFuture<>();
	}

	@Provides
	@ChooseMasterkeyFileScoped
	static Scene provideChooseMasterkeyScene(ChooseMasterkeyFileController controller, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle, @KeyLoading Stage window, @KeyLoading Vault v) {
		// TODO: simplify FxmlLoaderFactory
		try {
			var url = FxmlLoaderFactory.class.getResource(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE.getRessourcePathString());
			var loader = new FXMLLoader(url, resourceBundle, null, clazz -> controller);
			Parent root = loader.load();
			var scene = sceneFactory.apply(root);
			scene.windowProperty().addListener((prop, oldVal, newVal) -> {
				if (window.equals(newVal)) {
					window.setTitle(String.format(resourceBundle.getString("unlock.chooseMasterkey.title"), v.getDisplayName()));
				}
			});
			return scene;
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load UnlockScene", e);
		}
	}


}
