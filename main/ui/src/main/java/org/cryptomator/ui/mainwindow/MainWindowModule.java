package org.cryptomator.ui.mainwindow;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.cryptomator.ui.FxApplicationScoped;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.FxControllerKey;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultList;

@Module
public abstract class MainWindowModule {

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
	
	// ------------------

	@Provides
	@FxApplicationScoped
	@MainWindow
	static Stage providePrimaryStage() {
		Stage stage = new Stage();
		stage.setMinWidth(652.0);
		stage.setMinHeight(440.0);
		stage.initStyle(StageStyle.UNDECORATED);
		return stage;
	}

	@Binds
	abstract ObservableList<Vault> bindVaultList(VaultList vaultList);

	@Provides
	@FxApplicationScoped
	static ObjectProperty<Vault> provideSelectedVault() {
		return new SimpleObjectProperty<>();
	}

}
