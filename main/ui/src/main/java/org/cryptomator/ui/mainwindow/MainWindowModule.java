package org.cryptomator.ui.mainwindow;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.migration.MigrationComponent;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Module(subcomponents = {AddVaultWizardComponent.class, MigrationComponent.class, RemoveVaultComponent.class, VaultOptionsComponent.class, WrongFileAlertComponent.class})
abstract class MainWindowModule {

	@Provides
	@MainWindow
	@MainWindowScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@MainWindow
	@MainWindowScoped
	static Stage provideStage(@Named("windowIcon") Optional<Image> windowIcon) {
		Stage stage = new Stage();
		// TODO: min/max values chosen arbitrarily. We might wanna take a look at the user's resolution...
		stage.setMinWidth(650);
		stage.setMinHeight(440);
		stage.setMaxWidth(1000);
		stage.setMaxHeight(700);
		stage.initStyle(StageStyle.UNDECORATED);
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.MAIN_WINDOW)
	@MainWindowScoped
	static Scene provideMainScene(@MainWindow FXMLLoaderFactory fxmlLoaders, MainWindowController mainWindowController, VaultListController vaultListController) {
		Scene scene = fxmlLoaders.createScene("/fxml/main_window.fxml");

		// still not perfect... cant't we have a global menubar via the AWT tray app?
		KeyCombination cmdN = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
		KeyCombination cmdW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
		scene.getAccelerators().put(cmdN, vaultListController::didClickAddVault);
		scene.getAccelerators().put(cmdW, mainWindowController::close);

		return scene;
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
	@FxControllerKey(VaultDetailController.class)
	abstract FxController bindVaultDetailController(VaultDetailController controller);

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
	@FxControllerKey(VaultDetailNeedsMigrationController.class)
	abstract FxController bindVaultDetailNeedsMigrationController(VaultDetailNeedsMigrationController controller);

	@Binds
	@IntoMap
	@FxControllerKey(VaultListCellController.class)
	abstract FxController bindVaultListCellController(VaultListCellController controller);



}
