package org.cryptomator.ui.removevault;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class RemoveVaultModule {

	@Provides
	@RemoveVault
	@RemoveVaultScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@RemoveVault
	@RemoveVaultScoped
	static Stage provideStage(ResourceBundle resourceBundle) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("removeVault.title"));
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.REMOVE_VAULT)
	@RemoveVaultScoped
	static Scene provideUnlockScene(@RemoveVault FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/remove_vault.fxml"); // TODO rename fxml file
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(RemoveVaultController.class)
	abstract FxController bindRemoveVaultController(RemoveVaultController controller);
}
