package org.cryptomator.ui.recoverykey;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.NewPasswordController;
import org.cryptomator.ui.common.PasswordStrengthUtil;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class RecoveryKeyModule {

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle, @Named("keyRecoveryOwner") Stage owner) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("recoveryKey.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static StringProperty provideRecoveryKeyProperty() {
		return new SimpleStringProperty();
	}

	@Provides
	@RecoveryKeyScoped
	@Named("newPassword")
	static ObjectProperty<CharSequence> provideNewPasswordProperty() {
		return new SimpleObjectProperty<>("");
	}


	// ------------------

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_CREATE)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyCreationScene(@RecoveryKeyWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.RECOVERYKEY_CREATE);
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_SUCCESS)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeySuccessScene(@RecoveryKeyWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.RECOVERYKEY_SUCCESS);
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_RECOVER)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyRecoverScene(@RecoveryKeyWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.RECOVERYKEY_RECOVER);
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyResetPasswordScene(@RecoveryKeyWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyCreationController.class)
	abstract FxController bindRecoveryKeyCreationController(RecoveryKeyCreationController controller);

	@Provides
	@IntoMap
	@FxControllerKey(RecoveryKeyDisplayController.class)
	static FxController provideRecoveryKeyDisplayController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow StringProperty recoveryKey, ResourceBundle localization) {
		return new RecoveryKeyDisplayController(window, vault.getDisplayName(), recoveryKey.get(), localization);
	}

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyRecoverController.class)
	abstract FxController provideRecoveryKeyRecoverController(RecoveryKeyRecoverController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeySuccessController.class)
	abstract FxController bindRecoveryKeySuccessController(RecoveryKeySuccessController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyResetPasswordController.class)
	abstract FxController bindRecoveryKeyResetPasswordController(RecoveryKeyResetPasswordController controller);

	@Provides
	@IntoMap
	@FxControllerKey(NewPasswordController.class)
	static FxController provideNewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater, @Named("newPassword") ObjectProperty<CharSequence> password) {
		return new NewPasswordController(resourceBundle, strengthRater, password);
	}

}
