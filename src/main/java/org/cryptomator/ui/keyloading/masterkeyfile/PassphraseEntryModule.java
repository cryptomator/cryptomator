package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javafx.scene.Scene;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

@Module
interface PassphraseEntryModule {

	@Provides
	@PassphraseEntryScoped
	static CompletableFuture<PassphraseEntryResult> provideResult() {
		return new CompletableFuture<>();
	}

	@Provides
	@PassphraseEntryScoped
	static Scene provideUnlockScene(PassphraseEntryController controller, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return FxmlLoaderFactory.forController(controller, sceneFactory, resourceBundle).createScene(FxmlFile.UNLOCK_ENTER_PASSWORD);
	}

}
