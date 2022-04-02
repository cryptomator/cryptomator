package org.cryptomator.ui.lock;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Module
abstract class LockModule {

	@Provides
	@LockScoped
	static AtomicReference<CompletableFuture<Boolean>> provideForceRetryDecisionRef() {
		return new AtomicReference<>();
	}

	@Provides
	@LockWindow
	@LockScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@LockWindow
	@LockScoped
	static Stage provideWindow(StageFactory factory, @LockWindow Vault vault, @Nullable @Named("lockWindowOwner") Stage owner) {
		Stage stage = factory.create();
		stage.setTitle(vault.getDisplayName());
		stage.setResizable(false);
		if (owner != null) {
			stage.initOwner(owner);
			stage.initModality(Modality.WINDOW_MODAL);
		} else {
			stage.initModality(Modality.APPLICATION_MODAL);
		}
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.LOCK_FORCED)
	@LockScoped
	static Scene provideForceLockScene(@LockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.LOCK_FORCED);
	}

	@Provides
	@FxmlScene(FxmlFile.LOCK_FAILED)
	@LockScoped
	static Scene provideLockFailedScene(@LockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.LOCK_FAILED);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(LockForcedController.class)
	abstract FxController bindLockForcedController(LockForcedController controller);

	@Binds
	@IntoMap
	@FxControllerKey(LockFailedController.class)
	abstract FxController bindLockFailedController(LockFailedController controller);

}
