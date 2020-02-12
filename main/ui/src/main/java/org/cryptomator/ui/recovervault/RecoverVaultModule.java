package org.cryptomator.ui.recovervault;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class RecoverVaultModule {

	@Provides
	@RecoverVaultWindow
	@RecoverVaultScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@RecoverVaultWindow
	@RecoverVaultScoped
	static Stage provideStage(ResourceBundle resourceBundle, @Named("windowIcons") List<Image> windowIcons, @Named("recoverVaultOwner") Stage owner) {
		Stage stage = new Stage();
		//TODO stage.setTitle(resourceBundle.getString("recoverVault.title"));
		stage.setTitle("TODO recover Vault");
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.getIcons().addAll(windowIcons);
		return stage;
	}

	@Provides
	@RecoverVaultWindow
	@RecoverVaultScoped
	static StringProperty provideRecoveryKeyProperty() {
		return new SimpleStringProperty();
	}

	// ------------------

	@Provides
	@FxmlScene(FxmlFile.RECOVER_VAULT)
	@RecoverVaultScoped
	static Scene provideRecoverVaultScene(@RecoverVaultWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/recovervault.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(RecoverVaultController.class)
	abstract FxController bindRecoverVaultController(RecoverVaultController controller);

}
