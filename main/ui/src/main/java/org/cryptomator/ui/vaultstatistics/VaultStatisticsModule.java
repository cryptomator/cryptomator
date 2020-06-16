package org.cryptomator.ui.vaultstatistics;

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
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class VaultStatisticsModule {

	@Provides
	@VaultStatisticsWindow
	@VaultStatisticsScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@VaultStatisticsWindow
	@VaultStatisticsScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle, @VaultStatisticsWindow Vault vault) {
		Stage stage = factory.create();
		stage.setTitle(String.format(resourceBundle.getString("vaultstatistics.title"), vault.getDisplayableName()));
		stage.setResizable(false);
		vault.stateProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends VaultState> observable, VaultState oldValue, VaultState newValue) {
				if (newValue != VaultState.UNLOCKED) {
					stage.hide();
					observable.removeListener(this);
				}
			}
		});
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.VAULT_STATISTICS)
	@VaultStatisticsScoped
	static Scene provideVaultStatisticsScene(@VaultStatisticsWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/vault_statistics.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(VaultStatisticsController.class)
	abstract FxController bindVaultStatisticsController(VaultStatisticsController controller);
}
