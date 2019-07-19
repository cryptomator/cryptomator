package org.cryptomator.ui.mainwindow;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;

import javax.inject.Provider;
import java.util.Map;

@Module
public abstract class MainWindowModule {

	@Provides
	@MainWindowScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories) {
		return new FXMLLoaderFactory(factories);
	}

	@Provides
	@MainWindowScoped
	static Stage provideStage() {
		Stage stage = new Stage();
		stage.setMinWidth(652.0);
		stage.setMinHeight(440.0);
		stage.initStyle(StageStyle.UNDECORATED);
		return stage;
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

}
