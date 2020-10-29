package org.cryptomator.ui.addvaultwizard;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.NewPasswordController;
import org.cryptomator.ui.common.PasswordStrengthUtil;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;
import org.cryptomator.ui.recoverykey.RecoveryKeyDisplayController;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.Map;
import java.util.ResourceBundle;

@Module
public abstract class AddVaultModule {

	@Provides
	@AddVaultWizardScoped
	@Named("newPassword")
	static ObjectProperty<CharSequence> provideNewPasswordProperty() {
		return new SimpleObjectProperty<>("");
	}

	@Provides
	@AddVaultWizardWindow
	@AddVaultWizardScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@AddVaultWizardWindow
	@AddVaultWizardScoped
	static Stage provideStage(StageFactory factory, @MainWindow Stage owner, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("addvaultwizard.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@AddVaultWizardScoped
	static ObjectProperty<Path> provideVaultPath() {
		return new SimpleObjectProperty<>();
	}

	@Provides
	@Named("vaultName")
	@AddVaultWizardScoped
	static StringProperty provideVaultName() {
		return new SimpleStringProperty("");
	}

	@Provides
	@AddVaultWizardWindow
	@AddVaultWizardScoped
	static ObjectProperty<Vault> provideVault() {
		return new SimpleObjectProperty<>();
	}

	@Provides
	@Named("recoveryKey")
	@AddVaultWizardScoped
	static StringProperty provideRecoveryKey() {
		return new SimpleStringProperty();
	}

	// ------------------

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_WELCOME)
	@AddVaultWizardScoped
	static Scene provideWelcomeScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_WELCOME.getRessourcePathString());
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_EXISTING)
	@AddVaultWizardScoped
	static Scene provideChooseExistingVaultScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_EXISTING.getRessourcePathString());
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_NAME)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultNameScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_NEW_NAME.getRessourcePathString());
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultLocationScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_NEW_LOCATION.getRessourcePathString());
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultPasswordScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_NEW_PASSWORD.getRessourcePathString());
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_RECOVERYKEY)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultRecoveryKeyScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_NEW_RECOVERYKEY.getRessourcePathString());
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_SUCCESS)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultSuccessScene(@AddVaultWizardWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ADDVAULT_SUCCESS.getRessourcePathString());
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(AddVaultWelcomeController.class)
	abstract FxController bindWelcomeController(AddVaultWelcomeController controller);

	@Binds
	@IntoMap
	@FxControllerKey(ChooseExistingVaultController.class)
	abstract FxController bindChooseExistingVaultController(ChooseExistingVaultController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultNameController.class)
	abstract FxController bindCreateNewVaultNameController(CreateNewVaultNameController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultLocationController.class)
	abstract FxController bindCreateNewVaultLocationController(CreateNewVaultLocationController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultPasswordController.class)
	abstract FxController bindCreateNewVaultPasswordController(CreateNewVaultPasswordController controller);

	@Provides
	@IntoMap
	@FxControllerKey(NewPasswordController.class)
	static FxController provideNewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater, @Named("newPassword") ObjectProperty<CharSequence> password) {
		return new NewPasswordController(resourceBundle, strengthRater, password);
	}

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultRecoveryKeyController.class)
	abstract FxController bindCreateNewVaultRecoveryKeyController(CreateNewVaultRecoveryKeyController controller);

	@Provides
	@IntoMap
	@FxControllerKey(RecoveryKeyDisplayController.class)
	static FxController provideRecoveryKeyDisplayController(@AddVaultWizardWindow Stage window, @Named("vaultName") StringProperty vaultName, @Named("recoveryKey") StringProperty recoveryKey, ResourceBundle localization) {
		return new RecoveryKeyDisplayController(window, vaultName.get(), recoveryKey.get(), localization);
	}

	@Binds
	@IntoMap
	@FxControllerKey(AddVaultSuccessController.class)
	abstract FxController bindAddVaultSuccessController(AddVaultSuccessController controller);

}
