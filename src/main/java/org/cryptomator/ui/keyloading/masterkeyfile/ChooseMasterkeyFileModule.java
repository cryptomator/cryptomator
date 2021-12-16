package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

@Module
abstract class ChooseMasterkeyFileModule {

	@Provides
	@ChooseMasterkeyFileScoped
	static CompletableFuture<Path> provideResult() {
		return new CompletableFuture<>();
	}

	@Provides
	@ChooseMasterkeyFileScoped
	static Scene provideChooseMasterkeyScene(SelectMasterkeyFileController controller, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		// TODO: simplify FxmlLoaderFactory
		try {
			var url = FxmlLoaderFactory.class.getResource(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE.getRessourcePathString());
			var loader = new FXMLLoader(url, resourceBundle, null, clazz -> controller);
			Parent root = loader.load();
			return sceneFactory.apply(root);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load UnlockScene", e);
		}
	}


}
