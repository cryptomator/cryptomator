package org.cryptomator.ui.recoverykey;

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
import org.cryptomator.common.vaults.Vault;
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
abstract class RecoveryKeyModule {

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static Stage provideStage(ResourceBundle resourceBundle, @Named("windowIcons") List<Image> windowIcons, @Named("keyRecoveryOwner") Stage owner) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("recoveryKey.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.getIcons().addAll(windowIcons);
		return stage;
	}
	
	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static StringProperty provideRecoveryKeyProperty() {
		return new SimpleStringProperty();
	}
	
	// ------------------

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_CREATE)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyCreationScene(@RecoveryKeyWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/recoverykey_create.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_SUCCESS)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeySuccessScene(@RecoveryKeyWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/recoverykey_success.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_RECOVER)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyRecoverScene(@RecoveryKeyWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/recoverykey_recover.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyCreationController.class)
	abstract FxController bindRecoveryKeyCreationController(RecoveryKeyCreationController controller);

	@Provides
	@IntoMap
	@FxControllerKey(RecoveryKeyDisplayController.class)
	static FxController provideRecoveryKeyDisplayController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow StringProperty recoveryKey) {
		return new RecoveryKeyDisplayController(window, vault.getDisplayableName(), recoveryKey.get());
	}

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyRecoverController.class)
	abstract FxController provideRecoveryKeyRecoverController(RecoveryKeyRecoverController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeySuccessController.class)
	abstract FxController bindRecoveryKeySuccessController(RecoveryKeySuccessController controller);
	
}
