package org.cryptomator.ui.vaultstatistics;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;

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
	static Stage provideStage(StageFactory factory, @MainWindow Stage owner, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("vaultstatistics.title"));
		stage.setResizable(false);
		stage.initModality(Modality.NONE);
		stage.initOwner(owner);
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
