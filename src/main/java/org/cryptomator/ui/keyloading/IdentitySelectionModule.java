package org.cryptomator.ui.keyloading;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.VaultIdentity;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javafx.scene.Scene;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

@Module
abstract class IdentitySelectionModule {

	@Provides
	@IdentitySelectionScoped
	static CompletableFuture<VaultIdentity> provideResult() {
		return new CompletableFuture<>();
	}

	@Provides
	@IdentitySelectionScoped
	static Scene provideIdentitySelectionScene(IdentitySelectionController controller, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return FxmlLoaderFactory.forController(controller, sceneFactory, resourceBundle).createScene(FxmlFile.UNLOCK_SELECT_IDENTITY);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(IdentitySelectionController.class)
	abstract FxController bindIdentitySelectionController(IdentitySelectionController controller);

}
