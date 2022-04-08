package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javafx.scene.Scene;
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
	static Scene provideChooseMasterkeyScene(ChooseMasterkeyFileController controller, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return FxmlLoaderFactory.forController(controller, sceneFactory, resourceBundle).createScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE);
	}

}
