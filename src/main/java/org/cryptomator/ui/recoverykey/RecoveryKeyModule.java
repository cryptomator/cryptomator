package org.cryptomator.ui.recoverykey;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.addvaultwizard.CreateNewVaultExpertSettingsController;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.changepassword.PasswordStrengthUtil;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class RecoveryKeyModule {

	@Provides
	@Nullable
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static VaultConfig.UnverifiedVaultConfig vaultConfig(@RecoveryKeyWindow Vault vault) {
		try {
			return vault.getVaultConfigCache().get();
		} catch (IOException e) {
			return null;
		}
	}

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static Stage provideStage(StageFactory factory, @Named("keyRecoveryOwner") Stage owner) {
		Stage stage = factory.create();
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

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_ONBOARDING)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyOnboardingScene(@RecoveryKeyWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.RECOVERYKEY_ONBOARDING);
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyExpertSettingsScene(@RecoveryKeyWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS);
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

	@Provides
	@Named("shorteningThreshold")
	@RecoveryKeyScoped
	static IntegerProperty provideShorteningThreshold() {
		return new SimpleIntegerProperty(CreateNewVaultExpertSettingsController.MAX_SHORTENING_THRESHOLD);
	}

	@Provides
	@Named("cipherCombo")
	@RecoveryKeyScoped
	static ObjectProperty<CryptorProvider.Scheme> provideCipherCombo() {
		return new SimpleObjectProperty<>();
	}

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyExpertSettingsController.class)
	abstract FxController provideRecoveryKeyExpertSettingsController(RecoveryKeyExpertSettingsController controller);

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

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyOnboardingController.class)
	abstract FxController bindRecoveryKeyOnboardingController(RecoveryKeyOnboardingController controller);

	@Provides
	@IntoMap
	@FxControllerKey(RecoveryKeyValidateController.class)
	static FxController bindRecoveryKeyValidateController(@RecoveryKeyWindow Vault vault, @RecoveryKeyWindow @Nullable VaultConfig.UnverifiedVaultConfig vaultConfig, @RecoveryKeyWindow StringProperty recoveryKey, RecoveryKeyFactory recoveryKeyFactory, @Named("recoverType") ObjectProperty<RecoveryActionType>  recoverType, @Named("cipherCombo") ObjectProperty<CryptorProvider.Scheme> cipherCombo, @Nullable MasterkeyFileAccess masterkeyFileAccess) {
		return new RecoveryKeyValidateController(vault, vaultConfig, recoveryKey, recoveryKeyFactory, recoverType, cipherCombo, masterkeyFileAccess);
	}

	@Provides
	@IntoMap
	@FxControllerKey(NewPasswordController.class)
	static FxController provideNewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater) {
		return new NewPasswordController(resourceBundle, strengthRater);
	}

}
