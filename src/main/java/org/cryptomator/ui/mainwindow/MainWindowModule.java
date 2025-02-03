package org.cryptomator.ui.mainwindow;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.common.StageInitializer;
import org.cryptomator.ui.error.ErrorComponent;
import org.cryptomator.ui.fxapp.PrimaryStage;
import org.cryptomator.ui.migration.MigrationComponent;
import org.cryptomator.ui.stats.VaultStatisticsComponent;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module(subcomponents = {AddVaultWizardComponent.class, MigrationComponent.class, VaultStatisticsComponent.class, WrongFileAlertComponent.class, ErrorComponent.class})
abstract class MainWindowModule {

	@Provides
	@MainWindow
	@MainWindowScoped
	static Stage provideMainWindow(@PrimaryStage Stage stage, StageInitializer initializer) {
		initializer.accept(stage);
		stage.setTitle("Cryptomator");
		stage.setMinWidth(650);
		stage.setMinHeight(498);
		return stage;
	}

	@Provides
	@MainWindowScoped
	static ObjectProperty<Vault> provideSelectedVault() {
		return new SimpleObjectProperty<>();
	}

	@Provides
	@MainWindow
	@MainWindowScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, MainWindowSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@MainWindowScoped
	@Named("errorWindow")
	static Stage provideErrorStage(@MainWindow Stage window, StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("main.vaultDetail.error.windowTitle"));
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(window);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.MAIN_WINDOW)
	@MainWindowScoped
	static Scene provideMainScene(@MainWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.MAIN_WINDOW);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(MainWindowController.class)
	abstract FxController bindMainWindowController(MainWindowController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultListController.class)
	abstract FxController bindVaultListController(VaultListController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultListContextMenuController.class)
	abstract FxController bindVaultListContextMenuController(VaultListContextMenuController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultDetailController.class)
	abstract FxController bindVaultDetailController(VaultDetailController controller);

	@Binds
	@IntoMap
	@FxControllerKey(WelcomeController.class)
	abstract FxController bindWelcomeController(WelcomeController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultDetailLockedController.class)
	abstract FxController bindVaultDetailLockedController(VaultDetailLockedController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultDetailUnlockedController.class)
	abstract FxController bindVaultDetailUnlockedController(VaultDetailUnlockedController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultDetailMissingVaultController.class)
	abstract FxController bindVaultDetailMissingVaultController(VaultDetailMissingVaultController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultDetailNeedsMigrationController.class)
	abstract FxController bindVaultDetailNeedsMigrationController(VaultDetailNeedsMigrationController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultDetailUnknownErrorController.class)
	abstract FxController bindVaultDetailUnknownErrorController(VaultDetailUnknownErrorController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultListCellController.class)
	abstract FxController bindVaultListCellController(VaultListCellController controller);


}
