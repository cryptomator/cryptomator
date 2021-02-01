package org.cryptomator.ui.stats;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Provider;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class VaultStatisticsModule {

	@Provides
	@VaultStatisticsWindow
	@VaultStatisticsScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@VaultStatisticsWindow
	@VaultStatisticsScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle, @VaultStatisticsWindow Vault vault) {
		Stage stage = factory.create();
		stage.setTitle(String.format(resourceBundle.getString("stats.title"), vault.getDisplayName()));
		stage.setResizable(false);
		var weakStage = new WeakReference<>(stage);
		vault.stateProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends VaultState> observable, VaultState oldValue, VaultState newValue) {
				if (newValue != VaultState.UNLOCKED) {
					Stage stage = weakStage.get();
					if (stage != null) {
						stage.hide();
					}
					observable.removeListener(this);
				}
			}
		});
		stage.setOnCloseRequest(windowEvent -> vault.showingStatsProperty().setValue(false));
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.VAULT_STATISTICS)
	@VaultStatisticsScoped
	static Scene provideVaultStatisticsScene(@VaultStatisticsWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.VAULT_STATISTICS);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(VaultStatisticsController.class)
	abstract FxController bindVaultStatisticsController(VaultStatisticsController controller);
}
